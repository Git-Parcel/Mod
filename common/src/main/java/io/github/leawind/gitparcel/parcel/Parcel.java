package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.Constants;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class Parcel {
  public static final String PARCEL_META_FILE_NAME = "parcel.json";

  /**
   * Loads a parcel at the specified position in the specified level.
   *
   * @param level The level to load the parcel into
   * @param pos The position to load the parcel at
   * @param dir The parcel directory, which should contain the {@value #PARCEL_META_FILE_NAME} file
   * @param loadBlocks Whether to load the blocks in the parcel. Some formats may ignore it and
   *     always load blocks
   * @param loadEntities Whether to load the entities in the parcel
   * @throws IOException If an I/O error occurs while loading the parcel
   * @throws ParcelException.InvalidParcel If the parcel is invalid
   * @throws ParcelException If other error occurs while loading the parcel
   */
  public static void load(
      ServerLevel level, BlockPos pos, Path dir, boolean loadBlocks, boolean loadEntities)
      throws IOException, ParcelException {
    var metaFile = dir.resolve(PARCEL_META_FILE_NAME);
    var meta = ParcelMeta.load(metaFile);
    var loader = Constants.PARCEL_FORMATS.loader(meta.formatId, meta.formatVersion);
    if (loader == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }
    loader.load(level, pos, dir, loadBlocks, loadEntities);
  }

  /** Custom exception for parcel-related errors. */
  public static class ParcelException extends Exception {
    public ParcelException(String message) {
      super(message);
    }

    public ParcelException(String message, Throwable cause) {
      super(message, cause);
    }

    /** Exception thrown when a parcel format is invalid */
    public static class InvalidParcel extends ParcelException {
      public InvalidParcel(String message) {
        super(message);
      }

      public InvalidParcel(String message, Throwable cause) {
        super(message, cause);
      }
    }
  }
}
