package io.github.leawind.gitparcel.common.api.permission;

import com.mojang.serialization.Codec;

public final class ParcelPermissions {
  private ParcelPermissions() {}

  public static final PermissionTypeRegistry<ParcelPermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionConfig<ParcelPermissions>> CONFIG_CODEC =
      PermissionConfig.getMapCodec(REGISTRY);

  private static PermissionType<ParcelPermissions> type(String id, PermissionLevel defaultLevel) {
    return REGISTRY.register(new PermissionType<>(id, defaultLevel));
  }

  public static final PermissionType<ParcelPermissions> VIEW =
      type("view", PermissionLevel.MODERATORS);
  public static final PermissionType<ParcelPermissions> SAVE =
      type("save_snapshot", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> RESTORE =
      type("restore_snapshot", PermissionLevel.ADMINS);
  public static final PermissionType<ParcelPermissions> MANAGE_HISTORY =
      type("manage_history", PermissionLevel.OWNERS);
  public static final PermissionType<ParcelPermissions> CONFIG =
      type("config", PermissionLevel.OWNERS);
}
