package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.regex.Pattern;

public interface GitParcelPermission {
  Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

  PermissionTypeRegistry REGISTRY = new PermissionTypeRegistry();

  Codec<PermissionSettings> SETTINGS_CODEC = PermissionSettings.getCodec(REGISTRY);

  PermissionType LIST_FORMAT = type(0, "list_format", 1);
  PermissionType LIST_INSTANCE = type(1, "list_instance", 1);

  PermissionType SAVE_INSTANCE = type(2, "save_instance", 3);
  PermissionType LOAD_INSTANCE = type(3, "load_instance", 3);

  PermissionType NEW_INSTANCE = type(4, "new_instance", 4);
  PermissionType MOD_INSTANCE = type(5, "mod_instance", 4);
  PermissionType DEL_INSTANCE = type(6, "del_instance", 4);

  PermissionType COMMIT = type(7, "commit", 3);

  static boolean permits(int requiredLevel, int level) {
    return level >= requiredLevel;
  }

  static PermissionSettings settingsFrom(Map<String, Byte> map) {
    return PermissionSettings.from(REGISTRY, map);
  }

  static PermissionType type(int id, String name, int defaultLevel)
      throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    // Check name
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "name must match pattern " + NAME_PATTERN.pattern() + ", got: " + name);
    }

    // Check defaultLevel
    if (defaultLevel < 0) {
      throw new IllegalArgumentException("defaultLevel must be >= 0, got: " + defaultLevel);
    }
    if (defaultLevel > 4) {
      throw new IllegalArgumentException("defaultLevel must be <= 4, got: " + defaultLevel);
    }

    return REGISTRY.register(new PermissionType((byte) id, name, (byte) defaultLevel));
  }
}
