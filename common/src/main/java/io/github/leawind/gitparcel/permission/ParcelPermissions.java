package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import net.minecraft.server.permissions.PermissionLevel;

public class ParcelPermissions extends GitParcelPermissions {
  public static final PermissionTypeRegistry<ParcelPermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionConfig<ParcelPermissions>> CONFIG_CODEC =
      PermissionConfig.getMapCodec(REGISTRY);

  private static PermissionType<ParcelPermissions> type(
      int id, String name, PermissionLevel defaultLevel) {
    validateName(name);
    return REGISTRY.register(new PermissionType<>((byte) id, name, defaultLevel));
  }

  public static final PermissionType<ParcelPermissions> SAVE =
      type(2, "instance_save", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> LOAD =
      type(3, "instance_load", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> CONFIG =
      type(5, "instance_mod", PermissionLevel.OWNERS);
  public static final PermissionType<ParcelPermissions> COMMIT =
      type(7, "instance_commit", PermissionLevel.ADMINS);
}
