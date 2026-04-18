package io.github.leawind.gitparcel.world;

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
  private static final long DEFAULT_MAX_PARCEL_VOLUME = 128 * 128 * 128;

  public static final Codec<GitParcelWorldSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      WorldPermissions.CONFIG_CODEC
                          .fieldOf("permissions")
                          .forGetter(GitParcelWorldSavedData::permissions),
                      ParcelPermissions.CONFIG_CODEC
                          .fieldOf("parcel_default_permissions")
                          .forGetter(GitParcelWorldSavedData::parcelDefaultPermissions),
                      Codec.LONG
                          .optionalFieldOf("max_parcel_volume", DEFAULT_MAX_PARCEL_VOLUME)
                          .forGetter(GitParcelWorldSavedData::maxParcelVolume))
                  .apply(inst, GitParcelWorldSavedData::new));

  public static final SavedDataType<GitParcelWorldSavedData> TYPE =
      new SavedDataType<>(
          "gitparcel_world", GitParcelWorldSavedData::new, CODEC, DataFixTypes.LEVEL);

  private final PermissionConfig<WorldPermissions> permissions;
  private final PermissionConfig<ParcelPermissions> parcelDefaultPermissions;

  private long maxParcelVolume;

  public PermissionConfig<WorldPermissions> permissions() {
    return permissions;
  }

  public PermissionConfig<ParcelPermissions> parcelDefaultPermissions() {
    return parcelDefaultPermissions;
  }

  public long maxParcelVolume() {
    return maxParcelVolume;
  }

  public void maxParcelVolume(long maxParcelVolume) {
    this.maxParcelVolume = maxParcelVolume;
    setDirty();
  }

  private GitParcelWorldSavedData() {
    this(
        new PermissionConfig<>(WorldPermissions.REGISTRY),
        new PermissionConfig<>(ParcelPermissions.REGISTRY),
        DEFAULT_MAX_PARCEL_VOLUME);
  }

  private GitParcelWorldSavedData(
      PermissionConfig<WorldPermissions> permissions,
      PermissionConfig<ParcelPermissions> parcelDefaultPermissions,
      long maxParcelVolume) {
    this.permissions = permissions;
    this.parcelDefaultPermissions = parcelDefaultPermissions;
    this.maxParcelVolume = maxParcelVolume;
  }

  public static GitParcelWorldSavedData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(TYPE);
  }

  public void reset() {
    permissions.clearAll();
    parcelDefaultPermissions.clearAll();
    maxParcelVolume = DEFAULT_MAX_PARCEL_VOLUME;
    setDirty();
  }
}
