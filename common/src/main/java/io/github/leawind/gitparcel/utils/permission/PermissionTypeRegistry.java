package io.github.leawind.gitparcel.utils.permission;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jspecify.annotations.Nullable;

/**
 * A registry for {@link PermissionType}s, indexed by id.
 *
 * <p>Types are registered at class-load time via {@link #register(PermissionType)}.
 *
 * @param <T> A type-safety tag, used to distinguish between different registries
 */
public final class PermissionTypeRegistry<T> {
  private final Object2ObjectMap<String, PermissionType<T>> byId = new Object2ObjectArrayMap<>();

  public PermissionTypeRegistry() {}

  /**
   * Returns the registered type by its string id, or {@code null} if not found.
   *
   * @param id the permission type id
   */
  public @Nullable PermissionType<T> byId(String id) {
    return byId.get(id);
  }

  /**
   * Registers a permission type. The id must be unique within this registry.
   *
   * @param type the permission type to register
   * @return the registered type
   * @throws IllegalArgumentException if the id is already taken
   */
  public PermissionType<T> register(PermissionType<T> type) {
    if (byId.containsKey(type.id())) {
      throw new IllegalArgumentException("id must be unique in this registry: " + type.id());
    }
    byId.put(type.id(), type);

    return type;
  }
}
