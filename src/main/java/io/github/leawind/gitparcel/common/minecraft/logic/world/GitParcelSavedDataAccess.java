package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Owns Minecraft's version-specific saved-data type declarations and storage access.
 *
 * <p>Callers and state models only depend on the stable {@link #level(ServerLevel)} and
 * {@link #world(MinecraftServer)} methods.
 */
@VersionSensitive("Minecraft SavedData type and storage API")
final class GitParcelSavedDataAccess {
  /**
   * Minecraft has no generic custom-data DFU type. Command storage uses a remainder schema and has
   * no content-specific fixes, so it safely preserves Git Parcel's independently versioned data.
   * A non-null value is required by the vanilla/Fabric SavedData storage implementation.
   */
  private static final DataFixTypes PASS_THROUGH_DATA_FIX =
      DataFixTypes.SAVED_DATA_COMMAND_STORAGE;

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

  private GitParcelSavedDataAccess() {}

  static GitParcelLevelSavedData level(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(LEVEL_TYPE);
  }

  static GitParcelWorldSavedData world(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(WORLD_TYPE);
  }
}
