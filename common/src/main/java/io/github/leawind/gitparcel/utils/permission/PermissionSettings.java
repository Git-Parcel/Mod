package io.github.leawind.gitparcel.utils.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import java.util.Map;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

public class PermissionSettings<T> {
  private final PermissionTypeRegistry<T> registry;

  /** Map from permission type id to required level */
  private final Int2ObjectMap<PermissionLevel> requirements = new Int2ObjectArrayMap<>();

  public PermissionSettings(PermissionTypeRegistry<T> registry) {
    this.registry = registry;
  }

  public PermissionTypeRegistry<T> getRegistry() {
    return registry;
  }

  public boolean isSpecified(PermissionType<T> type) {
    return requirements.containsKey(type.id());
  }

  public PermissionLevel get(PermissionType<T> type) {
    var level = requirements.get(type.id());
    return level == null ? type.defaultLevel() : level;
  }

  public Object2ByteMap<String> toMap() {
    Object2ByteMap<String> map = new Object2ByteArrayMap<>();
    requirements.forEach(
        (id, level) -> {
          var type = registry.byId(id);
          if (type != null) {
            map.put(type.name(), (byte) level.id());
          }
        });
    return map;
  }

  public void clear(PermissionType<T> type) {
    requirements.remove(type.id());
  }

  public void set(PermissionType<T> type, int levelId) {
    set(type, PermissionLevel.byId(levelId));
  }

  public void set(PermissionType<T> type, PermissionLevel level) {
    requirements.put(type.id(), level);
  }

  public boolean permits(PermissionType<T> type, PermissionLevel level) {
    return level.isEqualOrHigherThan(get(type));
  }

  public boolean permits(PermissionType<T> type, PermissionSet set) {
    return getChecker(get(type)).check(set);
  }

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
   * @param map Permission type name --> Permission level requirement
   */
  public static <T> PermissionSettings<T> from(
      PermissionTypeRegistry<T> registry, Map<String, Byte> map) {
    PermissionSettings<T> settings = new PermissionSettings<>(registry);
    map.forEach(
        (name, levelId) -> {
          var type = registry.byName(name);
          if (type != null) {
            settings.set(type, PermissionLevel.byId(levelId));
          }
        });
    return settings;
  }

  public static <T> Codec<PermissionSettings<T>> getMapCodec(PermissionTypeRegistry<T> registry) {
    return Codec.unboundedMap(Codec.STRING, Codec.BYTE)
        .xmap(map -> from(registry, map), PermissionSettings::toMap);
  }
}
