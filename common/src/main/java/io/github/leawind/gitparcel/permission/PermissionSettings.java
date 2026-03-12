package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import java.util.Map;
import net.minecraft.server.permissions.PermissionLevel;

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
