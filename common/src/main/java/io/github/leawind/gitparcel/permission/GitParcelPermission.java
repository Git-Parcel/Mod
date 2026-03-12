package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import java.util.Map;

public interface GitParcelPermission {
  PermissionTypeRegistry REGISTRY = new PermissionTypeRegistry();

  Codec<PermissionSettings> SETTINGS_CODEC =
      Codec.unboundedMap(Codec.STRING, Codec.BYTE)
          .xmap(GitParcelPermission::from, PermissionSettings::toMap);

  PermissionType LIST_FORMAT = REGISTRY.register(0, "list_format", 1);
  PermissionType LIST_INSTANCE = REGISTRY.register(1, "list_instance", 1);

  PermissionType SAVE_INSTANCE = REGISTRY.register(2, "save_instance", 3);
  PermissionType LOAD_INSTANCE = REGISTRY.register(3, "load_instance", 3);

  PermissionType NEW_INSTANCE = REGISTRY.register(4, "new_instance", 4);
  PermissionType MOD_INSTANCE = REGISTRY.register(5, "mod_instance", 4);
  PermissionType DEL_INSTANCE = REGISTRY.register(6, "del_instance", 4);

  PermissionType COMMIT = REGISTRY.register(7, "commit", 3);

  static boolean permits(int requiredLevel, int level) {
    return level >= requiredLevel;
  }

  static PermissionSettings from(Map<String, Byte> map) {
    return PermissionSettings.from(REGISTRY, map);
  }
}
