package io.github.leawind.gitparcel.common.impl.parcel;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class ParcelContentTypeRegistryImpl implements ParcelContentTypeRegistry {
  public static final ParcelContentTypeRegistryImpl INSTANCE =
      new ParcelContentTypeRegistryImpl();

  private final Map<ParcelContentType.Spec, ParcelContentType<?>> registrations =
      new LinkedHashMap<>();
  private List<ParcelContentType<?>> latest = List.of();
  private boolean frozen;

  ParcelContentTypeRegistryImpl() {}

  @Override
  public void register(ParcelContentType<?> type) {
    ensureMutable();
    if (registrations.putIfAbsent(type.spec(), type) != null) {
      throw new IllegalArgumentException("Duplicate parcel content type: " + type.spec());
    }
  }

  @Override
  public @Nullable ParcelContentType<?> get(ParcelContentType.Spec spec) {
    return registrations.get(spec);
  }

  @Override
  public @Nullable ParcelContentType<?> latest(String id) {
    return registrations.values().stream()
        .filter(type -> type.spec().id().equals(id))
        .max(Comparator.comparingInt(type -> type.spec().version()))
        .orElse(null);
  }

  @Override
  public List<ParcelContentType<?>> latestTypes() {
    return frozen ? latest : computeLatest();
  }

  @Override
  public List<ParcelContentType<?>> registeredTypes() {
    return registrations.values().stream()
        .sorted(Comparator.comparing(ParcelContentType::spec))
        .toList();
  }

  @Override
  public void clear() {
    ensureMutable();
    registrations.clear();
    latest = List.of();
  }

  @Override
  public void freeze() {
    if (frozen) return;
    latest = computeLatest();
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  private List<ParcelContentType<?>> computeLatest() {
    Map<String, ParcelContentType<?>> byId = new LinkedHashMap<>();
    for (ParcelContentType<?> type : registrations.values()) {
      byId.merge(
          type.spec().id(),
          type,
          (left, right) -> left.spec().version() > right.spec().version() ? left : right);
    }
    var result = new ArrayList<>(byId.values());
    result.sort(Comparator.comparing(type -> type.spec().id()));
    return List.copyOf(result);
  }

  private void ensureMutable() {
    if (frozen) {
      throw new IllegalStateException("Parcel content type registry is frozen");
    }
  }
}
