package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import net.minecraft.server.permissions.PermissionLevel;

public class WorldPermissions extends GitParcelPermission {
  public static final PermissionTypeRegistry REGISTRY = new PermissionTypeRegistry();
  public static final Codec<PermissionSettings> SETTINGS_CODEC =
      PermissionSettings.getMapCodec(REGISTRY);

  private static PermissionType type(int id, String name, PermissionLevel defaultLevel) {
    validateName(name);
    return REGISTRY.register(new PermissionType((byte) id, name, defaultLevel));
  }

  public static final PermissionType LIST_FORMAT =
      type(0, "list_format", PermissionLevel.MODERATORS);
  public static final PermissionType LIST_INSTANCE =
      type(1, "list_instance", PermissionLevel.MODERATORS);
  public static final PermissionType CREATE_PARCEL_INSTANCE =
      type(4, "create_instance", PermissionLevel.OWNERS);
  public static final PermissionType DEL_INSTANCE = type(6, "del_instance", PermissionLevel.OWNERS);
}
