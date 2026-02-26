package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.Constants;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Parcel {
  public static final String PARCEL_META_FILE_NAME = "parcel.json";

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * <p>It loads metadata file {@value #PARCEL_META_FILE_NAME} first and try to save as the same
   * format
   *
   * @param level The level to save the parcel in
   * @param pos The position to save the parcel at
   * @param size The size of the parcel
   * @param dir The parcel directory, which should contain the {@value #PARCEL_META_FILE_NAME} file
   * @param saveEntity Whether to save the entities in the parcel
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  public static void save(Level level, BlockPos pos, Vec3i size, Path dir, boolean saveEntity)
      throws IOException, ParcelException {
    var metaFile = dir.resolve(PARCEL_META_FILE_NAME);
    var meta = ParcelMeta.load(metaFile);
    var format = Constants.PARCEL_FORMATS.saver(meta.formatId, meta.formatVersion);
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }
    meta.size = size;
    meta.save(metaFile);
    format.save(level, pos, size, dir, meta.includeEntity() && saveEntity);
  }

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
    var meta = ParcelMeta.load(dir.resolve(PARCEL_META_FILE_NAME));
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
