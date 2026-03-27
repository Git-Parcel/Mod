package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import net.minecraft.server.permissions.PermissionLevel;

public class WorldPermissions {
  public static final PermissionTypeRegistry<WorldPermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionConfig<WorldPermissions>> CONFIG_CODEC =
      PermissionConfig.getMapCodec(REGISTRY);

  private static PermissionType<WorldPermissions> type(String id, PermissionLevel defaultLevel) {
    return REGISTRY.register(new PermissionType<>(id, defaultLevel));
  }

  public static final PermissionType<WorldPermissions> LIST_FORMATS =
      type("list_formats", PermissionLevel.MODERATORS);
  public static final PermissionType<WorldPermissions> LIST_PARCELS =
      type("list_pararcels", PermissionLevel.MODERATORS);
  public static final PermissionType<WorldPermissions> CREATE_PARCEL =
      type("create_parcel", PermissionLevel.OWNERS);
  public static final PermissionType<WorldPermissions> DELETE_PARCEL =
      type("delete_parcel", PermissionLevel.OWNERS);
  public static final PermissionType<WorldPermissions> CONFIG_PARCEL =
      type("config_parcel", PermissionLevel.OWNERS);
}
