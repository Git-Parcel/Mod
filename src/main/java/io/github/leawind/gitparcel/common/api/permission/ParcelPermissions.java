package io.github.leawind.gitparcel.common.api.permission;

import com.mojang.serialization.Codec;
import net.minecraft.server.permissions.PermissionLevel;

public class ParcelPermissions {
  public static final PermissionTypeRegistry<ParcelPermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionConfig<ParcelPermissions>> CONFIG_CODEC =
      PermissionConfig.getMapCodec(REGISTRY);

  private static PermissionType<ParcelPermissions> type(String id, PermissionLevel defaultLevel) {
    return REGISTRY.register(new PermissionType<>(id, defaultLevel));
  }

  public static final PermissionType<ParcelPermissions> SAVE = type("save", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> LOAD = type("load", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> CONFIG =
      type("config", PermissionLevel.OWNERS);
  public static final PermissionType<ParcelPermissions> COMMIT =
      type("commit", PermissionLevel.ADMINS);
}
