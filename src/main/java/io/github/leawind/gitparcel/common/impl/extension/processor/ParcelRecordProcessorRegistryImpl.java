package io.github.leawind.gitparcel.common.impl.extension.processor;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ParcelRecordProcessorRegistryImpl implements ParcelRecordProcessorRegistry {
  public static final ParcelRecordProcessorRegistryImpl INSTANCE =
      new ParcelRecordProcessorRegistryImpl();

  private final Map<Identifier, ParcelRecordProcessor> processors = new LinkedHashMap<>();
  private List<ParcelRecordProcessor> ordered = List.of();
  private boolean frozen;

  ParcelRecordProcessorRegistryImpl() {}

  @Override
  public void register(ParcelRecordProcessor processor) {
    ensureMutable();
    if (processors.putIfAbsent(processor.id(), processor) != null) {
      throw new IllegalArgumentException("duplicate parcel data processor: " + processor.id());
    }
  }

  @Override
  public @Nullable ParcelRecordProcessor get(Identifier id) {
    return processors.get(id);
  }

  @Override
  public List<ParcelRecordProcessor> orderedProcessors() {
    if (!frozen) {
      throw new IllegalStateException("Parcel data processor registry is not frozen");
    }
    return ordered;
  }

  @Override
  public void freeze() {
    if (frozen) return;
    ordered = topologicalOrder();
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  private List<ParcelRecordProcessor> topologicalOrder() {
    Map<Identifier, Set<Identifier>> outgoing = new HashMap<>();
    Map<Identifier, Integer> incoming = new HashMap<>();
    processors.keySet().forEach(id -> {
      outgoing.put(id, new HashSet<>());
      incoming.put(id, 0);
    });

    for (var processor : processors.values()) {
      for (Identifier dependency : processor.runAfter()) {
        requirePresent(processor.id(), dependency);
        addEdge(dependency, processor.id(), outgoing, incoming);
      }
      for (Identifier successor : processor.runBefore()) {
        requirePresent(processor.id(), successor);
        addEdge(processor.id(), successor, outgoing, incoming);
      }
    }

    var ready =
        new java.util.PriorityQueue<Identifier>(Comparator.comparing(Identifier::toString));
    incoming.forEach((id, count) -> {
      if (count == 0) ready.add(id);
    });
    var result = new ArrayList<ParcelRecordProcessor>(processors.size());
    while (!ready.isEmpty()) {
      Identifier id = ready.remove();
      result.add(processors.get(id));
      for (Identifier next : outgoing.get(id)) {
        int count = incoming.compute(next, (ignored, old) -> old - 1);
        if (count == 0) ready.add(next);
      }
    }
    if (result.size() != processors.size()) {
      throw new IllegalStateException("Cycle in parcel data processor ordering");
    }
    return List.copyOf(result);
  }

  private void requirePresent(Identifier owner, Identifier dependency) {
    if (!processors.containsKey(dependency)) {
      throw new IllegalStateException(
          "Processor %s requires missing processor %s".formatted(owner, dependency));
    }
  }

  private static void addEdge(
      Identifier from,
      Identifier to,
      Map<Identifier, Set<Identifier>> outgoing,
      Map<Identifier, Integer> incoming) {
    if (outgoing.get(from).add(to)) {
      incoming.compute(to, (ignored, old) -> old + 1);
    }
  }

  private void ensureMutable() {
    if (frozen) {
      throw new IllegalStateException("Parcel data processor registry is frozen");
    }
  }
}
