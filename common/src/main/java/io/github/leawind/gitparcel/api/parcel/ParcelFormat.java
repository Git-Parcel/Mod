package io.github.leawind.gitparcel.api.parcel;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** A format for saving or loading parcels. */
public sealed interface ParcelFormat<C extends ParcelFormatConfig<C>>
    permits ParcelFormat.Save, ParcelFormat.Load, ParcelFormat.Impl {
  Logger LOGGER = LogUtils.getLogger();
  String META_FILE_NAME = "parcel.json";
  String CONFIG_FILE_NAME = "config.json";
  String DATA_DIR_NAME = "data";

  /** Path to {@value #META_FILE_NAME} in {@code parcelDir} */
  static Path getMetaFile(Path parcelDir) {
    return parcelDir.resolve(META_FILE_NAME);
  }

  /** Path to {@value #CONFIG_FILE_NAME} in {@code parcelDir} */
  static Path getConfigFile(Path parcelDir) {
    return parcelDir.resolve(CONFIG_FILE_NAME);
  }

  /** Path to {@value #DATA_DIR_NAME} in {@code parcelDir} */
  static Path getDataDir(Path parcelDir) {
    return parcelDir.resolve(DATA_DIR_NAME);
  }

  /**
   * Saves a parcel at the specified position in the specified level.
   *
   * @param transform
   * @param meta The metadata of the parcel. Will be updated to the size of the parcel.
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory. Will be created if not exists.
   * @param ignoreEntities Whether to ignore entities when saving the parcel
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   */
  @SuppressWarnings("unchecked")
  static <C extends ParcelFormatConfig<C>> void save(
      Level level,
      Parcel parcel,
      ParcelTransform transform,
      ParcelMeta meta,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    meta.size = parcel.getSize();
    meta.save(getMetaFile(parcelDir));

    var format = (Save<C>) meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    var config = format.getDefaultConfig();
    if (config != null) {
      var configFile = getConfigFile(parcelDir);
      if (Files.exists(configFile)) {
        try {
          config.load(configFile);
        } catch (Exception e) {
          LOGGER.error("Failed to load format config: {}", e.getMessage(), e);
          config.resetToDefault();
          config.save(configFile);
        }
      } else {
        config.save(configFile);
      }
    }

    format.save(
        level,
        parcel,
        transform,
        getDataDir(parcelDir),
        ignoreEntities && meta.excludeEntities(),
        config);
  }

  /**
   * Loads a parcel at the specified position in the specified level.
   *
   * @param level The level to load the parcel into
   * @param parcelOrigin Position of parcel origin in level
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param ignoreBlocks Whether to ignore blocks when loading the parcel
   * @param ignoreEntities Whether to ignore entities when loading the parcel
   * @param flags Flags to pass to {@link Level#setBlock} when loading blocks
   * @throws IOException If an I/O error occurs while loading the parcel
   * @throws ParcelException.InvalidParcel If the parcel is invalid
   * @throws ParcelException If other error occurs while loading the parcel
   */
  @SuppressWarnings("unchecked")
  static <C extends ParcelFormatConfig<C>> void load(
      ServerLevel level,
      BlockPos parcelOrigin,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags)
      throws IOException, ParcelException {
    var meta = ParcelMeta.load(parcelDir.resolve(META_FILE_NAME));
    Load<C> loader = (Load<C>) meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException("Unsupported format: " + meta.formatId + ":" + meta.formatVersion);
    }

    Path configFile = getConfigFile(parcelDir);
    C config = loader.getDefaultConfig();
    if (config != null && Files.exists(configFile)) {
      try {
        config.load(configFile);
      } catch (Exception e) {
        LOGGER.error("Failed to load format config: {}", e.getMessage(), e);
      }
    }

    Parcel parcel = new Parcel(parcelOrigin, meta.size);
    Path dataDir = parcelDir.resolve(DATA_DIR_NAME);
    loader.load(level, parcel, dataDir, ignoreBlocks, ignoreEntities, flags, config);
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

  default @Nullable Class<C> configClass() {
    return null;
  }

  /**
   * @return might be null
   */
  default @Nullable C getDefaultConfig() {
    return null;
  }

  class BaseContext {
    public final Level level;
    public final Parcel parcel;
    public final Path dataDir;

    public BaseContext(Level level, Parcel parcel, Path dataDir) {
      this.level = level;
      this.parcel = parcel;
      this.dataDir = dataDir;
    }
  }

  class SaveContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final boolean ignoreEntities;
    public final C config;

    public SaveContext(Level level, Parcel parcel, Path dataDir, boolean ignoreEntities, C config) {
      super(level, parcel, dataDir);
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }
  }

  class LoadContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final boolean ignoreBlocks;
    public final boolean ignoreEntities;
    public final C config;

    public LoadContext(
        ServerLevel level,
        Parcel parcel,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        C config) {
      super(level, parcel, dataDir);
      this.ignoreBlocks = ignoreBlocks;
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }
  }

  non-sealed interface Save<C extends ParcelFormatConfig<C>> extends ParcelFormat<C> {

    /**
     * Save parcel content to directory.
     *
     * <p>For implementation, you should save the parcel content to the {@code dataDir} directory
     * and nowhere else.
     *
     * @param level Level
     * @param parcel Parcel to save.
     * @param transform Treat the parcel as transformed.
     * @param dataDir Path to parcel data directory. Will be created if not exist.
     * @param ignoreEntities Whether to ignore entities in the parcel
     * @param config format config, can be null
     */
    void save(
        Level level,
        Parcel parcel,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException;
  }

  non-sealed interface Load<C extends ParcelFormatConfig<C>> extends ParcelFormat<C> {

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param parcel Parcel to load.
     * @param dataDir Path to parcel data directory
     * @param ignoreBlocks Whether to ignore blocks
     * @param ignoreEntities Whether to ignore entities
     * @param flags Block update flags
     */
    void load(
        ServerLevel level,
        Parcel parcel,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable C config)
        throws IOException, ParcelException;
  }

  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat<C> {}
}
