package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Resolves deterministic save and load orders from content-type dependencies. */
public final class ParcelContentTypeOrder {
  private ParcelContentTypeOrder() {}

  public static List<ParcelContentType<?>> forLoad(Collection<ParcelContentType<?>> types) {
    Map<String, ParcelContentType<?>> byId = new HashMap<>();
    for (ParcelContentType<?> type : types) {
      if (byId.putIfAbsent(type.spec().id(), type) != null) {
        throw new IllegalArgumentException("Multiple versions selected for " + type.spec().id());
      }
    }

    Map<String, Set<String>> outgoing = new HashMap<>();
    Map<String, Integer> incoming = new HashMap<>();
    byId.keySet().forEach(
        id -> {
          outgoing.put(id, new HashSet<>());
          incoming.put(id, 0);
        });

    for (ParcelContentType<?> type : byId.values()) {
      for (String dependency : type.loadAfter()) {
        if (!byId.containsKey(dependency)) {
          throw new IllegalStateException(
              "Parcel content type %s requires missing content %s"
                  .formatted(type.spec(), dependency));
        }
        if (outgoing.get(dependency).add(type.spec().id())) {
          incoming.compute(type.spec().id(), (ignored, count) -> count + 1);
        }
      }
    }

    var ready = new PriorityQueue<String>();
    incoming.forEach(
        (id, count) -> {
          if (count == 0) ready.add(id);
        });
    var result = new ArrayList<ParcelContentType<?>>(types.size());
    while (!ready.isEmpty()) {
      String id = ready.remove();
      result.add(byId.get(id));
      for (String next : outgoing.get(id)) {
        int count = incoming.compute(next, (ignored, old) -> old - 1);
        if (count == 0) ready.add(next);
      }
    }
    if (result.size() != types.size()) {
      throw new IllegalStateException("Cycle in parcel content load dependencies");
    }
    return List.copyOf(result);
  }

  public static List<ParcelContentType<?>> forSave(Collection<ParcelContentType<?>> types) {
    var result = new ArrayList<>(forLoad(types));
    java.util.Collections.reverse(result);
    return List.copyOf(result);
  }
}
