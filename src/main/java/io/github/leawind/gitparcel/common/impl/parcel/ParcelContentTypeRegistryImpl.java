package io.github.leawind.gitparcel.common.impl.parcel;

import io.github.leawind.gitparcel.common.api.extension.RegistrationSource;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParcelContentTypeRegistryImpl implements ParcelContentTypeRegistry {
  public static final ParcelContentTypeRegistryImpl INSTANCE =
      new ParcelContentTypeRegistryImpl();
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ParcelContentTypeRegistryImpl.class);

  private record Registration(RegistrationSource source, ParcelContentType<?> type) {}

  private final Map<ParcelContentType.Spec, Registration> registrations = new LinkedHashMap<>();
  private List<ParcelContentType<?>> latest = List.of();
  private boolean frozen;

  ParcelContentTypeRegistryImpl() {}

  /**
   * Content type ids are plain directory names without a namespace, so every duplicate
   * registration is adjudicated as a guest: priority first, then the extension id order.
   */
  @Override
  public void register(RegistrationSource source, ParcelContentType<?> type) {
    ensureMutable();
    var existing = registrations.get(type.spec());
    if (existing != null && !source.supersedes(existing.source())) {
      LOGGER.warn(
          "Parcel content type {} from extension {} was superseded by {} (rule 7.4)",
          type.spec(),
          source.extensionId(),
          existing.source().extensionId());
      return;
    }
    if (existing != null) {
      LOGGER.warn(
          "Parcel content type {} from extension {} supersedes {} (rule 7.4)",
          type.spec(),
          source.extensionId(),
          existing.source().extensionId());
    }
    registrations.put(type.spec(), new Registration(source, type));
  }

  @Override
  public @Nullable ParcelContentType<?> get(ParcelContentType.Spec spec) {
    var registration = registrations.get(spec);
    return registration == null ? null : registration.type();
  }

  @Override
  public @Nullable ParcelContentType<?> latest(String id) {
    return registrations.values().stream()
        .map(Registration::type)
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
        .map(Registration::type)
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
    for (ParcelContentType<?> type : registrations.values().stream().map(Registration::type).toList()) {
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
