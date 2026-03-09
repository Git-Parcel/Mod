package io.github.leawind.gitparcel.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A format for saving or loading parcels. */
public sealed interface ParcelFormat permits ParcelFormat.Impl, ParcelFormat.Info {
  Codec<ParcelFormat> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.STRING.fieldOf("id").forGetter(ParcelFormat::id),
                      Codec.INT.fieldOf("version").forGetter(ParcelFormat::version))
                  .apply(inst, Info::new));

  /** Unique id of the format. */
  String id();

  /** Version of the format. */
  int version();

  Logger LOGGER = LoggerFactory.getLogger("ParcellaFormat");
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
   * Saves a parcel instance to the specified directory.
   *
   * <p>The position is specified in transform, and the size is specified in meta.
   *
   * @param transform Parcel transformation
   * @param meta The metadata of the parcel.
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory. Will be created if not exists.
   * @param ignoreEntities Whether to ignore entities when saving the parcel
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   * @throws ParcelException.UnsupportedFormat If the format is not supported
   */
  @SuppressWarnings("unchecked")
  static <C extends ParcelFormatConfig<C>> void save(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    meta.save(getMetaFile(parcelDir));

    ParcelFormat.Save<C> format = (Save<C>) meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(meta.format());
    }

    var config = format.getDefaultConfig();
    if (config != null) {
      var configFile = getConfigFile(parcelDir);
      if (Files.exists(configFile)) {
        try {
          config.load(configFile);
        } catch (Exception e) {
          LOGGER.error(
              "Failed to load format config, use default and overwrite: {}", e.getMessage(), e);
          config.resetToDefault();
          config.save(configFile);
        }
      } else {
        config.save(configFile);
      }
    }

    format.save(
        level,
        meta.size(),
        transform,
        getDataDir(parcelDir),
        ignoreEntities && meta.excludeEntities(),
        config);
  }

  /**
   * Loads a parcel at the specified position in the specified level.
   *
   * @param level The level to load the parcel into
   * @param transform Parcel transformation, indicating the position and orientation of the parcel
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory
   * @param ignoreBlocks Whether to ignore blocks when loading the parcel
   * @param ignoreEntities Whether to ignore entities when loading the parcel
   * @param flags Flags to pass to {@link Level#setBlock} when loading blocks
   * @throws IOException If an I/O error occurs while loading the parcel
   * @throws ParcelException.CorruptedParcelException If the parcel is invalid and cannot be loaded
   * @throws ParcelException.UnsupportedFormat If the format is not supported
   */
  @SuppressWarnings("unchecked")
  static <C extends ParcelFormatConfig<C>> void load(
      ServerLevel level,
      ParcelTransform transform,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags)
      throws IOException, ParcelException {
    var meta = ParcelMeta.load(parcelDir.resolve(META_FILE_NAME));
    Load<C> loader = (Load<C>) meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException.UnsupportedFormat(meta.format());
    }

    Path configFile = getConfigFile(parcelDir);
    C config = loader.getDefaultConfig();
    if (config != null && Files.exists(configFile)) {
      try {
        config.load(configFile);
      } catch (Exception e) {
        LOGGER.error(
            "Failed to load format config, use default and continue: {}", e.getMessage(), e);
      }
    }

    Path dataDir = parcelDir.resolve(DATA_DIR_NAME);
    loader.load(
        level, meta.size(), transform, dataDir, ignoreBlocks, ignoreEntities, flags, config);
  }

  class BaseContext {
    public final Vec3i parcelSize;
    public final ParcelTransform transform;
    public final Path dataDir;

    public BaseContext(Vec3i parcelSize, ParcelTransform transform, Path dataDir) {
      this.parcelSize = parcelSize;
      this.transform = transform;
      this.dataDir = dataDir;
    }
  }

  class SaveContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final LevelAccessor level;
    public final boolean ignoreEntities;
    public final C config;

    public SaveContext(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        C config) {
      super(parcelSize, transform, dataDir);
      this.level = level;
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }
  }

  class LoadContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final ServerLevelAccessor level;
    public final boolean ignoreBlocks;
    public final boolean ignoreEntities;
    public final @Block.UpdateFlags int flags;
    public final C config;

    public LoadContext(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        C config) {
      super(parcelSize, transform, dataDir);
      this.level = level;
      this.ignoreBlocks = ignoreBlocks;
      this.ignoreEntities = ignoreEntities;
      this.flags = flags;
      this.config = config;
    }
  }

  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat {
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
  }

  interface Save<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Save parcel content to directory.
     *
     * <p>For implementation, you should save the parcel content to the {@code dataDir} directory
     * and nowhere else.
     *
     * @param level Level
     * @param parcelSize Real size of the parcel (the one saved in the disk, without transform)
     * @param transform Treat the parcel instance we are going to save as transformed.
     * @param dataDir Path to parcel data directory. Will be created if not exist.
     * @param ignoreEntities Whether to ignore entities in the parcel
     * @param config format config, can be null
     */
    void save(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException;
  }

  interface Load<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param size Real size of the parcel (the one saved in the disk, without transform)
     * @param transform Transform the parcel when loading.
     * @param dataDir Path to parcel data directory
     * @param ignoreBlocks Whether to ignore blocks
     * @param ignoreEntities Whether to ignore entities
     * @param flags Block update flags
     * @throws ParcelException.CorruptedParcelException If the parcel is invalid and cannot be
     *     loaded
     */
    void load(
        ServerLevelAccessor level,
        Vec3i size,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException;
  }

  record Info(String id, int version) implements ParcelFormat {}
}
