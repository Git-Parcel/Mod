package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import java.util.Map;

public class PermissionSettings implements ReadablePermissionSettings {

  public final PermissionTypeRegistry registry;

  /** Indicates which permission is specified */
  private long specified = 0L;

  /** Permission Type ID --> Required Level */
  private final byte[] requirements = new byte[64];

  public PermissionSettings(PermissionTypeRegistry registry) {
    this.registry = registry;
  }

  private boolean isSpecified(long mask) {
    return (specified & mask) != 0;
  }

  public boolean isSpecified(PermissionType type) {
    return isSpecified(type.mask());
  }

  @Override
  public PermissionTypeRegistry getRegistry() {
    return registry;
  }

  @Override
  public byte get(PermissionType type) {
    if (!isSpecified(type)) {
      return type.defaultLevel();
    }
    return requirements[type.id()];
  }

  public Object2ByteMap<String> toMap() {
    Object2ByteMap<String> map = new Object2ByteArrayMap<>();
    int len = requirements.length;
    for (int i = 0; i < len; i++) {
      var type = registry.byId(i);
      if (type != null && isSpecified(type)) {
        map.put(type.name(), requirements[i]);
      }
    }
    return map;
  }

  public void clear(PermissionType type) {
    specified &= ~type.mask();
    requirements[type.id()] = 0;
  }

  public void set(PermissionType type, int level) {
    specified |= type.mask();
    requirements[type.id()] = (byte) level;
  }

  public static PermissionSettings from(PermissionTypeRegistry registry, Map<String, Byte> map) {
    PermissionSettings settings = new PermissionSettings(registry);
    map.forEach(
        (name, level) -> {
          var type = registry.byName(name);
          if (type != null) {
            settings.set(type, level);
          }
        });
    return settings;
  }

  public static Codec<PermissionSettings> getCodec(PermissionTypeRegistry registry) {
    return Codec.unboundedMap(Codec.STRING, Codec.BYTE)
        .xmap(map -> from(registry, map), PermissionSettings::toMap);
  }
}
