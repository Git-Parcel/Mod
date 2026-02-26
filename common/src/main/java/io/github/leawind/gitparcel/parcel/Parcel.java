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
   * @param pos The position to save the parcel at
   * @param size The size of the parcel
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param saveEntity Whether to save the entities in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  public static void save(Level level, BlockPos pos, Vec3i size, Path parcelDir, boolean saveEntity)
      throws IOException, ParcelException {

    // Save metadata
    var metaFile = getMetaFile(parcelDir);
    var meta = ParcelMeta.load(metaFile);
    var format = meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    if (!meta.size.equals(size)) {
      meta.size = size;
      meta.save(metaFile);
    }
    format.save(level, pos, meta.size, getDataDir(parcelDir), meta.includeEntity() && saveEntity);
  }

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * @param level The level where the parcel is in
   * @param pos The position to save the parcel at
   * @param meta The metadata of the parcel
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param saveEntity Whether to save the entities in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  public static void save(
      Level level, BlockPos pos, ParcelMeta meta, Path parcelDir, boolean saveEntity)
      throws IOException, ParcelException {
    meta.save(getMetaFile(parcelDir));
    var format = meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }
    format.save(level, pos, meta.size, getDataDir(parcelDir), meta.includeEntity() && saveEntity);
  }

  /**
   * Loads a parcel at the specified position in the specified level.
   *
   * @param level The level to load the parcel into
   * @param pos The position to load the parcel at
   * @param dir The parcel directory, which should contain the {@value #META_FILE_NAME} file
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
    var meta = ParcelMeta.load(dir.resolve(META_FILE_NAME));
    var loader = meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    var dataDir = dir.resolve(DATA_DIR_NAME);
    loader.load(level, pos, dataDir, loadBlocks, loadEntities);
  }
}
