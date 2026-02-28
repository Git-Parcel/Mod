package io.github.leawind.gitparcel.parcel;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** A format for saving or loading parcels. */
public interface ParcelFormat<C extends ParcelFormatConfig<C>> {
  Logger LOGGER = LogUtils.getLogger();
  String META_FILE_NAME = "parcel.json";
  String CONFIG_FILE_NAME = "config.json";
  String DATA_DIR_NAME = "data";

  /**
   * Returns the path of the metadata file in the specified parcel directory.
   *
   * @param parcelDir The parcel directory
   */
  static Path getMetaFile(Path parcelDir) {
    return parcelDir.resolve(META_FILE_NAME);
  }

  static Path getConfigFile(Path parcelDir) {
    return parcelDir.resolve(CONFIG_FILE_NAME);
  }

  /**
   * Returns the path of the data directory in the specified parcel directory.
   *
   * @param parcelDir The parcel directory
   */
  static Path getDataDir(Path parcelDir) {
    return parcelDir.resolve(DATA_DIR_NAME);
  }

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * @param meta The metadata of the parcel. Will be updated to the size of the parcel.
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory. Will be created if not exists.
   * @param saveEntity Whether to save the entities in the parcel. Only works when {@code
   *     meta.includeEntity} is true
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  @SuppressWarnings("unchecked")
  static <C extends ParcelFormatConfig<C>> void save(
      Level level, Parcel parcel, ParcelMeta meta, Path parcelDir, boolean saveEntity)
      throws IOException, ParcelException {
    meta.size = parcel.getSize();
    meta.save(getMetaFile(parcelDir));

    var format = (Save<C>) meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    var config = format.getDefaultConfig();
    try {
      config.setFromJsonFile(getConfigFile(parcelDir));
    } catch (Exception e) {
      LOGGER.error("Failed to load format config: {}", e.getMessage(), e);
      config.resetToDefault();
    }

    format.save(level, parcel, getDataDir(parcelDir), meta.includeEntity() && saveEntity, config);
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
  static void load(
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

  /** Unique id of the format. */
  String id();

  /** Version of the format. */
  int version();

  /**
   * Safely casts a config object to the current format's configuration type.
   *
   * @param config The config object to cast, may be null
   * @param <T> The type of the input config object
   * @return The cast config object, or null if both configClass() returns null and config is null
   * @throws ClassCastException If configClass() returns null but config is non-null, or if the
   *     config object cannot be cast to the target type
   */
  default <T> C castConfig(T config) throws ClassCastException {
    var clazz = configClass();
    if (clazz != null) {
      return clazz.cast(config);
    }
    if (config == null) {
      return null;
    }
    throw new ClassCastException("Expected null, got {}" + config);
  }

  default Class<C> configClass() {
    return null;
  }

  default C getDefaultConfig() {
    return null;
  }

  interface Save<C extends ParcelFormatConfig<C>> extends ParcelFormat<C> {

    /**
     * Save parcel content to directory.
     *
     * <p>For implementation, you should save the parcel content to the {@code dataDir} directory
     * and nowhere else.
     *
     * @param level Level
     * @param parcel Parcel to save.
     * @param dataDir Path to parcel data directory. Will be created if not exist.
     * @param saveEntities Whether to save entities in the parcel
     */
    void save(Level level, Parcel parcel, Path dataDir, boolean saveEntities, @Nullable C config)
        throws IOException;
  }

  interface Load<C extends ParcelFormatConfig<C>> extends ParcelFormat<C> {

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param parcelOrigin Position of parcel origin in level
     * @param dataDir Path to parcel data directory
     * @param loadBlocks Whether to load blocks
     * @param loadEntities Whether to load entities
     */
    void load(
        ServerLevel level,
        BlockPos parcelOrigin,
        Path dataDir,
        boolean loadBlocks,
        boolean loadEntities)
        throws IOException, ParcelException;
  }
}
