package io.github.leawind.gitparcel.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a serialization format for reading and writing parcel data.
 *
 * <p>This is the main entry point for all parcel serialization operations. Implementations provide
 * format-specific logic for saving world data to disk and loading it back into the world.
 *
 * <p>ParcelFormat uses a sealed interface hierarchy:
 *
 * <ul>
 *   <li>{@link ParcelFormat} - Base interface with common utilities
 *   <li>{@link Impl} - Base interface for format implementations
 *   <li>{@link Saver} - Interface for formats that support saving
 *   <li>{@link Loader} - Interface for formats that support loading
 * </ul>
 *
 * <p>A format may implement just Save, just Load, or both interfaces. This allows for read-only or
 * write-only format implementations.
 */
public sealed interface ParcelFormat permits ParcelFormat.Impl {
  Logger LOGGER = LoggerFactory.getLogger("Parcel Format");

  /** Filename for parcel metadata JSON file */
  String META_FILE_NAME = "parcel.json";

  /** Filename for format-specific configuration JSON file */
  String CONFIG_FILE_NAME = "config.json";

  /** Directory name for storing format-specific parcel data */
  String DATA_DIR_NAME = "data";

  /**
   * Returns the identifying information for this format.
   *
   * @return format info containing id and version
   */
  Info info();

  /**
   * Returns the unique identifier of this format.
   *
   * @return format id string
   */
  default String id() {
    return info().id();
  }

  /**
   * Returns the version number of this format implementation.
   *
   * @return format version integer
   */
  default int version() {
    return info().version();
  }

  /**
   * Immutable identifying information for a parcel format.
   *
   * <p>Combines a human-readable identifier with a version number to uniquely identify a specific
   * format implementation. This is used for format registration and compatibility checking.
   *
   * @param id Unique string identifier for the format
   * @param version Version number of the format implementation
   * @see ParcelFormatRegistry
   */
  record Info(String id, int version) {
    public static final Codec<Info> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.fieldOf("id").forGetter(Info::id),
                        Codec.INT.fieldOf("version").forGetter(Info::version))
                    .apply(inst, Info::new));
    public static final Pattern ID_PATTERN =
        Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

    /**
     * @param id The unique id of the format, must match {@link Info#ID_PATTERN}
     * @param version The version of the format
     */
    public Info {
      if (!ID_PATTERN.matcher(id).matches()) {
        throw new IllegalArgumentException("ID must match " + ID_PATTERN);
      }
    }

    @NonNull
    @Override
    public String toString() {
      return String.format("%s:%d", id, version);
    }
  }

  /**
   * Base interface for all concrete parcel format implementations.
   *
   * <p>Provides configuration handling capabilities common to both saving and loading operations.
   * Format implementations should not directly implement this interface, but rather implement
   * {@link Saver} and/or {@link Loader} interfaces as appropriate.
   *
   * @param <C> The configuration type used by this format
   */
  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat {
    /**
     * Safely casts an arbitrary config object to this format's specific configuration type.
     *
     * @param config The config object to cast
     * @param <T> The source type of the config object
     * @return The config object cast to the appropriate type
     * @throws ClassCastException If the config object cannot be cast, or if this format does not
     *     support configuration and a non-null config was provided
     */
    default @NonNull <T> C castConfig(@NonNull T config) throws ClassCastException {
      var clazz = configClass();
      if (clazz == null) {
        throw new ClassCastException(
            String.format("Expected null, got %s: %s", config.getClass().getSimpleName(), config));
      }
      return clazz.cast(config);
    }

    /**
     * Returns the runtime class of this format's configuration type.
     *
     * @return configuration class, or null if this format does not use configuration
     */
    default @Nullable Class<C> configClass() {
      return null;
    }

    /**
     * Creates and returns a new configuration instance with default values.
     *
     * @return new default configuration instance, or null if this format does not use configuration
     */
    default @Nullable C getDefaultConfig() {
      return null;
    }
  }

  /**
   * Interface for parcel formats that support saving operations.
   *
   * <p>Implementations of this interface can write world data from a game level into the
   * standardized parcel directory structure.
   *
   * @param <C> The configuration type used by this format
   */
  interface Saver<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Writes parcel content from the world into the specified data directory.
     *
     * <p>Implementation contract: All data must be written exclusively to the provided {@code
     * dataDir} directory. Implementations must not modify any other files or directories.
     *
     * @param level Source game level to read blocks and entities from
     * @param parcelSize Original dimensions of the parcel before transformation
     * @param anchor Offset point that defines the parcel's origin relative to its bounds
     * @param transform Transformation to apply to coordinates before reading from the world
     * @param dataDir Directory where format-specific data should be written
     * @param ignoreEntities When true, entities should not be included in the saved output
     * @param config Format-specific configuration, may be null
     * @throws IOException If any I/O error occurs during writing
     */
    void save(
        Level level,
        Vec3i parcelSize,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException;
  }

  /**
   * Interface for parcel formats that support loading operations.
   *
   * <p>Implementations of this interface can read parcel data from disk and place it into a game
   * world.
   *
   * @param <C> The configuration type used by this format
   */
  interface Loader<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Reads parcel content from disk and places it into the target game level.
     *
     * @param level Target level where blocks and entities will be placed
     * @param size Original dimensions of the parcel as stored on disk
     * @param anchor Offset point that defines the parcel's origin relative to its bounds
     * @param transform Transformation to apply when placing the parcel in the world
     * @param dataDir Directory containing the format-specific parcel data
     * @param ignoreBlocks When true, blocks will not be placed into the world. Not guaranteed to be
     *     supported by all formats.
     * @param ignoreEntities When true, entities will not be spawned into the world. Guaranteed to
     *     be supported by all formats.
     * @param flags Block update flags to use when placing blocks. Usually {@code
     *     Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE |
     *     Block.UPDATE_SKIP_ALL_SIDEEFFECTS}
     * @param config Format-specific configuration, may be null
     * @throws IOException If any I/O error occurs during reading
     * @throws ParcelException.CorruptedParcelException If the parcel data is invalid or corrupted
     */
    void load(
        ServerLevelAccessor level,
        Vec3i size,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException;
  }

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
   * Base context object containing common parameters for parcel operations.
   *
   * <p>This class encapsulates the shared parameters that are used by both save and load
   * operations, eliminating redundant parameter passing.
   */
  class BaseContext {
    /** Original dimensions of the parcel */
    public final Vec3i parcelSize;

    /** Transformation to apply to the parcel */
    public final ParcelTransform transform;

    /** Directory containing format-specific data */
    public final Path dataDir;

    /** Origin anchor point of the parcel */
    public final Vec3i anchor;

    public BaseContext(Vec3i parcelSize, ParcelTransform transform, Path dataDir, Vec3i anchor) {
      this.parcelSize = parcelSize;
      this.transform = transform;
      this.dataDir = dataDir;
      this.anchor = anchor;
    }
  }

  /**
   * Context object containing all parameters required for a parcel save operation.
   *
   * @param <C> The configuration type used by the format
   */
  class SaveContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    /** Source level to read world data from */
    public final LevelAccessor level;

    /** Flag indicating if entities should be excluded */
    public final boolean ignoreEntities;

    /** Format-specific configuration */
    public final C config;

    public SaveContext(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreEntities,
        C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }
  }

  /**
   * Context object containing all parameters required for a parcel load operation.
   *
   * @param <C> The configuration type used by the format
   */
  class LoadContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    /** Target level to place the parcel into */
    public final ServerLevelAccessor level;

    /** Flag indicating if blocks should be skipped */
    public final boolean ignoreBlocks;

    /** Flag indicating if entities should be skipped */
    public final boolean ignoreEntities;

    /** Block update flags for block placement */
    public final @Block.UpdateFlags int flags;

    /** Format-specific configuration */
    public final C config;

    public LoadContext(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.ignoreBlocks = ignoreBlocks;
      this.ignoreEntities = ignoreEntities;
      this.flags = flags;
      this.config = config;
    }
  }

  /**
   * Saves a parcel to the specified directory.
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
      @Nullable C config,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    meta.save(getMetaFile(parcelDir));

    Saver<C> format = (Saver<C>) meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatInfo());
    }

    C actualConfig = config;
    if (actualConfig == null) {
      actualConfig = format.getDefaultConfig();
    }

    if (actualConfig != null) {
      var configFile = getConfigFile(parcelDir);
      if (Files.exists(configFile)) {
        try {
          actualConfig.load(configFile);
        } catch (Exception e) {
          LOGGER.error(
              "Failed to load format config, use default and overwrite: {}", e.getMessage(), e);
          actualConfig.resetToDefault();
          actualConfig.save(configFile);
        }
      } else {
        actualConfig.save(configFile);
      }
    }

    format.save(
        level,
        meta.size(),
        meta.anchor(),
        transform,
        getDataDir(parcelDir),
        ignoreEntities && meta.excludeEntities(),
        actualConfig);
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
    Loader<C> loader = (Loader<C>) meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatInfo());
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
        level,
        meta.size(),
        meta.anchor(),
        transform,
        dataDir,
        ignoreBlocks,
        ignoreEntities,
        flags,
        config);
  }
}
