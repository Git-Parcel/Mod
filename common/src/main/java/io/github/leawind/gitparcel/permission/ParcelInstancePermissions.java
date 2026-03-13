package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import net.minecraft.server.permissions.PermissionLevel;

public class ParcelInstancePermissions extends GitParcelPermission {
  public static final PermissionTypeRegistry<ParcelInstancePermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionConfig<ParcelInstancePermissions>> CONFIG_CODEC =
      PermissionConfig.getMapCodec(REGISTRY);

  private static PermissionType<ParcelInstancePermissions> type(
      int id, String name, PermissionLevel defaultLevel) {
    validateName(name);
    return REGISTRY.register(new PermissionType<>((byte) id, name, defaultLevel));
  }

  public static final PermissionType<ParcelInstancePermissions> SAVE =
      type(2, "instance_save", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelInstancePermissions> LOAD =
      type(3, "instance_load", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelInstancePermissions> CONFIG =
      type(5, "instance_mod", PermissionLevel.OWNERS);
  public static final PermissionType<ParcelInstancePermissions> COMMIT =
      type(7, "instance_commit", PermissionLevel.ADMINS);
}
