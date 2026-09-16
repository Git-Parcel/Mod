package io.github.leawind.gitparcel.common.impl.extension.processor;

import io.github.leawind.gitparcel.common.api.extension.RegistrationSource;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParcelRecordProcessorRegistryImpl implements ParcelRecordProcessorRegistry {
  public static final ParcelRecordProcessorRegistryImpl INSTANCE =
      new ParcelRecordProcessorRegistryImpl();
  private static final Logger LOGGER = LoggerFactory.getLogger(ParcelRecordProcessorRegistryImpl.class);

  private record Registration(RegistrationSource source, ParcelRecordProcessor processor) {}

  private final Map<Identifier, Registration> registrations = new LinkedHashMap<>();
  private List<ParcelRecordProcessor> ordered = List.of();
  private boolean frozen;

  ParcelRecordProcessorRegistryImpl() {}

  @Override
  public void register(RegistrationSource source, ParcelRecordProcessor processor) {
    ensureMutable();
    var existing = registrations.get(processor.id());
    if (existing != null && !source.supersedes(existing.source())) {
      LOGGER.warn(
          "Parcel processor {} from extension {} was superseded by {} (rule 7.4)",
          processor.id(),
          source.extensionId(),
          existing.source().extensionId());
      return;
    }
    if (existing != null) {
      LOGGER.warn(
          "Parcel processor {} from extension {} supersedes {} (rule 7.4)",
          processor.id(),
          source.extensionId(),
          existing.source().extensionId());
    }
    registrations.put(processor.id(), new Registration(source, processor));
  }

  @Override
  public @Nullable ParcelRecordProcessor get(Identifier id) {
    var registration = registrations.get(id);
    return registration == null ? null : registration.processor();
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
    registrations.keySet().forEach(id -> {
      outgoing.put(id, new HashSet<>());
      incoming.put(id, 0);
    });

    for (var registration : registrations.values()) {
      var processor = registration.processor();
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
    var result = new ArrayList<ParcelRecordProcessor>(registrations.size());
    while (!ready.isEmpty()) {
      Identifier id = ready.remove();
      result.add(registrations.get(id).processor());
      for (Identifier next : outgoing.get(id)) {
        int count = incoming.compute(next, (ignored, old) -> old - 1);
        if (count == 0) ready.add(next);
      }
    }
    if (result.size() != registrations.size()) {
      throw new IllegalStateException("Cycle in parcel data processor ordering");
    }
    return List.copyOf(result);
  }

  private void requirePresent(Identifier owner, Identifier dependency) {
    if (!registrations.containsKey(dependency)) {
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
