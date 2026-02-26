package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class Parcel {
  public static final String META_FILE_NAME = "parcel.json";
  public static final String DATA_DIR_NAME = "data";

  public static Path getMetaFile(Path parcelDir) {
    return parcelDir.resolve(META_FILE_NAME);
  }

  public static Path getDataDir(Path parcelDir) {
    return parcelDir.resolve(DATA_DIR_NAME);
  }

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * <p>It loads metadata file {@value #META_FILE_NAME} first and try to save as the same format
   *
   * @param level The level where the parcel is in
   * @param parcelOrigin Position of parcel origin in level
   * @param parcelSize Size of the parcel, must be positive
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param saveEntity Whether to save the entities in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  public static void save(
      Level level, BlockPos parcelOrigin, Vec3i parcelSize, Path parcelDir, boolean saveEntity)
      throws IOException, ParcelException {

    // Save metadata
    var metaFile = getMetaFile(parcelDir);
    var meta = ParcelMeta.load(metaFile);
    var format = meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    if (!meta.size.equals(parcelSize)) {
      meta.size = parcelSize;
      meta.save(metaFile);
    }
    format.save(
        level, parcelOrigin, meta.size, getDataDir(parcelDir), meta.includeEntity() && saveEntity);
  }

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * @param level The level where the parcel is in
   * @param parcelOrigin Position of parcel origin in level
   * @param meta The metadata of the parcel
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param saveEntity Whether to save the entities in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  public static void save(
      Level level, BlockPos parcelOrigin, ParcelMeta meta, Path parcelDir, boolean saveEntity)
      throws IOException, ParcelException {
    meta.save(getMetaFile(parcelDir));
    var format = meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }
    format.save(
        level, parcelOrigin, meta.size, getDataDir(parcelDir), meta.includeEntity() && saveEntity);
  }

  /**
   * Loads a parcel at the specified position in the specified level.
   *
   * @param level The level to load the parcel into
   * @param parcelOrigin Position of parcel origin in level
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param loadBlocks Whether to load the blocks in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @param loadEntities Whether to load the entities in the parcel
   * @throws IOException If an I/O error occurs while loading the parcel
   * @throws ParcelException.InvalidParcel If the parcel is invalid
   * @throws ParcelException If other error occurs while loading the parcel
   */
  public static void load(
      ServerLevel level,
      BlockPos parcelOrigin,
      Path parcelDir,
      boolean loadBlocks,
      boolean loadEntities)
      throws IOException, ParcelException {
    var meta = ParcelMeta.load(parcelDir.resolve(META_FILE_NAME));
    var loader = meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    var dataDir = parcelDir.resolve(DATA_DIR_NAME);
    loader.load(level, parcelOrigin, dataDir, loadBlocks, loadEntities);
  }
}
