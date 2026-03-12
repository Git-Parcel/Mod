package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import java.util.Map;
import net.minecraft.server.permissions.PermissionLevel;

public class PermissionSettings {
  private final PermissionTypeRegistry registry;

  /** Map from permission type id to required level */
  private final Int2ObjectMap<PermissionLevel> requirements = new Int2ObjectArrayMap<>();

  public PermissionSettings(PermissionTypeRegistry registry) {
    this.registry = registry;
  }

  public PermissionTypeRegistry getRegistry() {
    return registry;
  }

  public boolean isSpecified(PermissionType type) {
    return requirements.containsKey(type.id());
  }

  public PermissionLevel get(PermissionType type) {
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

  public void clear(PermissionType type) {
    requirements.remove(type.id());
  }

  public void set(PermissionType type, int levelId) {
    set(type, PermissionLevel.byId(levelId));
  }

  public void set(PermissionType type, PermissionLevel level) {
    requirements.put(type.id(), level);
  }

  /**
   * @param map Permission type name --> Permission level requirement
   */
  public static PermissionSettings from(PermissionTypeRegistry registry, Map<String, Byte> map) {
    PermissionSettings settings = new PermissionSettings(registry);
    map.forEach(
        (name, levelId) -> {
          var type = registry.byName(name);
          if (type != null) {
            settings.set(type, PermissionLevel.byId(levelId));
          }
        });
    return settings;
  }

  public static Codec<PermissionSettings> getMapCodec(PermissionTypeRegistry registry) {
    return Codec.unboundedMap(Codec.STRING, Codec.BYTE)
        .xmap(map -> from(registry, map), PermissionSettings::toMap);
  }
}
