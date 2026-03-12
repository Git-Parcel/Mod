package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import net.minecraft.server.permissions.PermissionLevel;

public class ParcelInstancePermissions extends GitParcelPermission {
  public static final PermissionTypeRegistry REGISTRY = new PermissionTypeRegistry();
  public static final Codec<PermissionSettings> SETTINGS_CODEC =
      PermissionSettings.getMapCodec(REGISTRY);

  private static PermissionType type(int id, String name, PermissionLevel defaultLevel) {
    validateName(name);
    return REGISTRY.register(new PermissionType((byte) id, name, defaultLevel));
  }

  public static final PermissionType SAVE = type(2, "instance_save", PermissionLevel.ADMINS);
  public static final PermissionType LOAD = type(3, "instance_load", PermissionLevel.ADMINS);
  public static final PermissionType CONFIG = type(5, "instance_mod", PermissionLevel.OWNERS);
  public static final PermissionType COMMIT = type(7, "instance_commit", PermissionLevel.ADMINS);
}
