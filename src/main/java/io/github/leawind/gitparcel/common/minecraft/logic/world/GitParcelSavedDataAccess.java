package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
/*? if >=1.21.11 {*/
import net.minecraft.world.level.saveddata.SavedDataType;
/*?} else if >=1.20.5 {*/
/*import net.minecraft.world.level.saveddata.SavedData;
 *//*?}*/

/**
 * Owns Minecraft's version-specific saved-data type declarations and storage access.
 *
 * <p>1.21.11 introduced codec-backed {@link SavedDataType}; 26.1 changed its identifier from a
 * string to a namespaced identifier. Older versions use either {@code SavedData.Factory} or
 * separate decoder/supplier arguments. Callers and state models only depend on the stable
 * {@link #level(ServerLevel)} and {@link #world(MinecraftServer)} methods.
 */
@VersionSensitive("Minecraft SavedData type and storage API")
final class GitParcelSavedDataAccess {
  private static final String LEGACY_LEVEL_ID = GitParcel.MOD_ID + "_level";
  private static final String LEGACY_WORLD_ID = GitParcel.MOD_ID + "_world";

  /**
   * Minecraft has no generic custom-data DFU type. Command storage uses a remainder schema and has
   * no content-specific fixes, so it safely preserves Git Parcel's independently versioned data.
   * A non-null value is required by the vanilla/Fabric SavedData storage implementation.
   */
  /*? if >=1.20.5 {*/
  private static final DataFixTypes PASS_THROUGH_DATA_FIX =
      DataFixTypes.SAVED_DATA_COMMAND_STORAGE;
  /*?}*/

  /*? if >=26.1 {*/
  private static final SavedDataType<GitParcelLevelSavedData> LEVEL_TYPE =
      new SavedDataType<>(
          GitParcelUtils.identifier("level"),
          GitParcelLevelSavedData::new,
          GitParcelLevelSavedData.CODEC,
          PASS_THROUGH_DATA_FIX);

  private static final SavedDataType<GitParcelWorldSavedData> WORLD_TYPE =
      new SavedDataType<>(
          GitParcelUtils.identifier("world"),
          GitParcelWorldSavedData::new,
          GitParcelWorldSavedData.CODEC,
          PASS_THROUGH_DATA_FIX);
  /*?} else if >=1.21.11 {*/
  /*private static final SavedDataType<GitParcelLevelSavedData> LEVEL_TYPE =
      new SavedDataType<>(
          LEGACY_LEVEL_ID,
          GitParcelLevelSavedData::new,
          GitParcelLevelSavedData.CODEC,
          PASS_THROUGH_DATA_FIX);

  private static final SavedDataType<GitParcelWorldSavedData> WORLD_TYPE =
      new SavedDataType<>(
          LEGACY_WORLD_ID,
          GitParcelWorldSavedData::new,
          GitParcelWorldSavedData.CODEC,
          PASS_THROUGH_DATA_FIX);
  *//*?} else if >=1.20.5 {*/
  /*private static final SavedData.Factory<GitParcelLevelSavedData> LEVEL_TYPE =
      new SavedData.Factory<>(
          GitParcelLevelSavedData::new,
          (tag, registries) ->
              CodecSavedData.decode(GitParcelLevelSavedData.CODEC, tag, registries),
          PASS_THROUGH_DATA_FIX);

  private static final SavedData.Factory<GitParcelWorldSavedData> WORLD_TYPE =
      new SavedData.Factory<>(
          GitParcelWorldSavedData::new,
          (tag, registries) ->
              CodecSavedData.decode(GitParcelWorldSavedData.CODEC, tag, registries),
          PASS_THROUGH_DATA_FIX);
  *//*?}*/

  private GitParcelSavedDataAccess() {}

  static GitParcelLevelSavedData level(ServerLevel level) {
    /*? if >=1.21.11 {*/
    return level.getDataStorage().computeIfAbsent(LEVEL_TYPE);
    /*?} else if >=1.20.5 {*/
    /*return level.getDataStorage().computeIfAbsent(LEVEL_TYPE, LEGACY_LEVEL_ID);
    *//*?} else {*/
    /*return level
        .getDataStorage()
        .computeIfAbsent(
            tag -> CodecSavedData.decode(GitParcelLevelSavedData.CODEC, tag),
            GitParcelLevelSavedData::new,
            LEGACY_LEVEL_ID);
    *//*?}*/
  }

  static GitParcelWorldSavedData world(MinecraftServer server) {
    /*? if >=1.21.11 {*/
    return server.overworld().getDataStorage().computeIfAbsent(WORLD_TYPE);
    /*?} else if >=1.20.5 {*/
    /*return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(WORLD_TYPE, LEGACY_WORLD_ID);
    *//*?} else {*/
    /*return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            tag -> CodecSavedData.decode(GitParcelWorldSavedData.CODEC, tag),
            GitParcelWorldSavedData::new,
            LEGACY_WORLD_ID);
    *//*?}*/
  }
}
