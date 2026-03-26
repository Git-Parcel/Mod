package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.permission.ParcelPermissions;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class GitParcelWorldSavedData extends SavedData {
  public static final Codec<GitParcelWorldSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      WorldPermissions.CONFIG_CODEC
                          .fieldOf("permissions")
                          .forGetter(GitParcelWorldSavedData::getPermissions),
                      ParcelPermissions.CONFIG_CODEC
                          .fieldOf("parcel_default_permissions")
                          .forGetter(GitParcelWorldSavedData::getParcelDefaultPermissions))
                  .apply(inst, GitParcelWorldSavedData::new));

  public static final SavedDataType<GitParcelWorldSavedData> TYPE =
      new SavedDataType<>(
          "gitparcel_world", GitParcelWorldSavedData::new, CODEC, DataFixTypes.LEVEL);

  private final PermissionConfig<WorldPermissions> permissions;
  private final PermissionConfig<ParcelPermissions> parcelDefaultPermissions;

  public PermissionConfig<WorldPermissions> getPermissions() {
    return permissions;
  }

  public PermissionConfig<ParcelPermissions> getParcelDefaultPermissions() {
    return parcelDefaultPermissions;
  }

  private GitParcelWorldSavedData() {
    this(
        new PermissionConfig<>(WorldPermissions.REGISTRY),
        new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  private GitParcelWorldSavedData(
      PermissionConfig<WorldPermissions> permissions,
      PermissionConfig<ParcelPermissions> parcelDefaultPermissions) {
    this.permissions = permissions;
    this.parcelDefaultPermissions = parcelDefaultPermissions;
  }

  public static GitParcelWorldSavedData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(TYPE);
  }

  public void reset() {
    permissions.clearAll();
    parcelDefaultPermissions.clearAll();
    setDirty();
  }
}
