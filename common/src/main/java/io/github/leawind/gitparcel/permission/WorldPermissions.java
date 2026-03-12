package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.utils.permission.PermissionSettings;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import net.minecraft.server.permissions.PermissionLevel;

public class WorldPermissions extends GitParcelPermission {
  public static final PermissionTypeRegistry<WorldPermissions> REGISTRY =
      new PermissionTypeRegistry<>();
  public static final Codec<PermissionSettings<WorldPermissions>> SETTINGS_CODEC =
      PermissionSettings.getMapCodec(REGISTRY);

  private static PermissionType<WorldPermissions> type(
      int id, String name, PermissionLevel defaultLevel) {
    validateName(name);
    return REGISTRY.register(new PermissionType<>((byte) id, name, defaultLevel));
  }

  public static final PermissionType<WorldPermissions> LIST_FORMAT =
      type(0, "list_format", PermissionLevel.MODERATORS);
  public static final PermissionType<WorldPermissions> LIST_INSTANCE =
      type(1, "list_instance", PermissionLevel.MODERATORS);
  public static final PermissionType<WorldPermissions> CREATE_PARCEL_INSTANCE =
      type(4, "create_instance", PermissionLevel.OWNERS);
  public static final PermissionType<WorldPermissions> DEL_INSTANCE =
      type(6, "del_instance", PermissionLevel.OWNERS);
}
