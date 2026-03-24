package io.github.leawind.gitparcel.utils.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Map;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

/**
 * Stores per-type permission level requirements backed by a {@link PermissionTypeRegistry}.
 *
 * <p>Each requirement defaults to the type's {@link PermissionType#defaultLevel()}, but can be
 * overridden individually via {@link #set(PermissionType, PermissionLevel)}. Overrides are
 * persisted as {@code Map<String, Byte>} (permission id → level id) through {@link
 * #getMapCodec(PermissionTypeRegistry)}.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * // Create — each domain has one config instance
 * PermissionConfig<ParcelPermissions> config = new PermissionConfig<>(ParcelPermissions.REGISTRY);
 *
 * // Override a specific permission
 * config.set(ParcelPermissions.SAVE, PermissionLevel.MODERATORS);
 *
 * // Check if a player's permission set allows an action
 * boolean allowed = config.permits(ParcelPermissions.SAVE, source.permissions());
 * }</pre>
 *
 * @param <T> the type-safety tag, matching the corresponding registry and permission types
 */
public final class PermissionConfig<T> {
  private final PermissionTypeRegistry<T> registry;

  private final Object2ObjectMap<String, PermissionLevel> requirements =
      new Object2ObjectArrayMap<>();

  /** Creates a new config backed by the given registry. */
  public PermissionConfig(PermissionTypeRegistry<T> registry) {
    this.registry = registry;
  }

  /** Returns the backing permission type registry. */
  public PermissionTypeRegistry<T> getRegistry() {
    return registry;
  }

  /**
   * Returns whether an explicit requirement has been set for this type (as opposed to using the
   * type's default level).
   */
  public boolean isSpecified(PermissionType<T> type) {
    return requirements.containsKey(type.id());
  }

  /**
   * Returns the required level for the given type. If no explicit override has been set, returns
   * the type's {@link PermissionType#defaultLevel()}.
   */
  public PermissionLevel get(PermissionType<T> type) {
    var level = requirements.get(type.id());
    return level == null ? type.defaultLevel() : level;
  }

  /**
   * Serializes only the explicitly-set requirements to an id-to-level_id map.
   *
   * @return map from permission type id to required level id
   */
  public Object2ByteMap<String> toMap() {
    Object2ByteMap<String> map = new Object2ByteArrayMap<>();
    requirements.forEach(
        (id, level) -> {
          var type = registry.byId(id);
          if (type != null) {
            map.put(type.id(), (byte) level.id());
          }
        });
    return map;
  }

  /** Removes the explicit requirement for this type, reverting to its default level. */
  public void clear(PermissionType<T> type) {
    requirements.remove(type.id());
  }

  /**
   * Sets the required level for the given type by level id.
   *
   * @param type the permission type
   * @param levelId the level id (see {@link PermissionLevel#byId(int)})
   */
  public void set(PermissionType<T> type, int levelId) {
    set(type, PermissionLevel.byId(levelId));
  }

  /**
   * Sets the required level for the given type.
   *
   * @param type the permission type
   * @param level the required permission level
   */
  public void set(PermissionType<T> type, PermissionLevel level) {
    requirements.put(type.id(), level);
  }

  /**
   * Returns whether the given level meets or exceeds the requirement for this type.
   *
   * @param type the permission type to check
   * @param level the level to test
   * @see #permits(PermissionType, PermissionSet) for checking against a player's permission set
   */
  public boolean permits(PermissionType<T> type, PermissionLevel level) {
    return level.isEqualOrHigherThan(get(type));
  }

  /**
   * Returns whether the given permission set meets the requirement for this type. This is the
   * primary method used at runtime to check command permissions.
   *
   * @param type the permission type to check
   * @param set typically obtained from {@code CommandSourceStack.permissions()}
   */
  public boolean permits(PermissionType<T> type, PermissionSet set) {
    return getChecker(get(type)).check(set);
  }

  /**
   * Resolves the highest {@link PermissionLevel} granted by the given set, from {@link
   * PermissionLevel#ALL} up to {@link PermissionLevel#OWNERS}.
   */
  public static PermissionLevel levelOf(PermissionSet set) {
    if (Commands.LEVEL_OWNERS.check(set)) {
      return PermissionLevel.OWNERS;
    } else if (Commands.LEVEL_ADMINS.check(set)) {
      return PermissionLevel.ADMINS;
    } else if (Commands.LEVEL_GAMEMASTERS.check(set)) {
      return PermissionLevel.GAMEMASTERS;
    } else if (Commands.LEVEL_MODERATORS.check(set)) {
      return PermissionLevel.MODERATORS;
    } else if (Commands.LEVEL_ALL.check(set)) {
      return PermissionLevel.ALL;
    } else {
      return PermissionLevel.ALL;
    }
  }

  /**
   * Returns a {@link PermissionCheck} that passes when the given level is met. The check maps each
   * {@link PermissionLevel} to its corresponding vanilla {@link Commands} level check.
   */
  public static PermissionCheck getChecker(PermissionLevel level) {
    return switch (level) {
      case PermissionLevel.ALL -> Commands.LEVEL_ALL;
      case PermissionLevel.MODERATORS -> Commands.LEVEL_MODERATORS;
      case PermissionLevel.GAMEMASTERS -> Commands.LEVEL_GAMEMASTERS;
      case PermissionLevel.ADMINS -> Commands.LEVEL_ADMINS;
      case PermissionLevel.OWNERS -> Commands.LEVEL_OWNERS;
    };
  }

  /**
   * Deserializes a permission config from an id-to-level_id map. Entries whose ids are not found in
   * the registry are silently ignored.
   *
   * @param registry the registry to look up permission types
   * @param map permission type id to required level id
   */
  public static <T> PermissionConfig<T> from(
      PermissionTypeRegistry<T> registry, Map<String, Byte> map) {
    PermissionConfig<T> config = new PermissionConfig<>(registry);
    map.forEach(
        (id, levelId) -> {
          var type = registry.byId(id);
          if (type != null) {
            config.set(type, PermissionLevel.byId(levelId));
          }
        });
    return config;
  }

  /**
   * Returns a codec that round-trips a {@link PermissionConfig} as a {@code Map<String, Byte>}.
   *
   * @param registry the registry shared between serializer and deserializer
   */
  public static <T> Codec<PermissionConfig<T>> getMapCodec(PermissionTypeRegistry<T> registry) {
    return Codec.unboundedMap(Codec.STRING, Codec.BYTE)
        .xmap(map -> from(registry, map), PermissionConfig::toMap);
  }
}
