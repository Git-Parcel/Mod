package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelContentSink;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelContentSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParcelStorage {
  public static final Logger LOGGER = LoggerFactory.getLogger("Parcel Storage");

  private static final String META_FILE_NAME = "parcel.json";

  private static final String CONFIG_FILE_NAME = "config.json";
  private static final String DATA_DIR_NAME = "data";

  private static Path getMetaFile(Path parcelDir) {
    return parcelDir.resolve(META_FILE_NAME);
  }

  private static Path getConfigFile(Path parcelDir) {
    return parcelDir.resolve(CONFIG_FILE_NAME);
  }

  private static Path getDataDir(Path parcelDir) {
    return parcelDir.resolve(DATA_DIR_NAME);
  }

  /**
   * Resolves the directory used for a parcel's files.
   *
   * @param internalParcelsDir root directory for parcels stored inside the current world
   */
  public static Path resolveParcelDirectory(Parcel parcel, Path internalParcelsDir) {
    return parcel
        .location()
        .map(Parcel.ParcelLocation::getParcelPath)
        .orElseGet(
            () -> internalParcelsDir.resolve(parcel.uuid().toString()).resolve("parcel"));
  }

  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void save(
      Level level, Parcel parcel, Path parcelDir, boolean ignoreEntities)
      throws IOException, ParcelException {
    C config = null;
    var serializedConfig = parcel.formatConfig().orElse(null);
    if (serializedConfig != null) {
      ParcelFormat.Writer<C> format =
          (ParcelFormat.Writer<C>)
              ParcelFormatRegistry.get().getWriter(parcel.meta().formatSpec());
      if (format == null) {
        throw new ParcelException.UnsupportedFormat(parcel.meta().formatSpec());
      }

      config = format.getDefaultConfig();
      if (config != null) {
        config.setFromJson(serializedConfig.getAsJsonObject());
      }
    }

    save(
        level,
        parcel.transform(),
        parcel.meta(),
        config,
        parcelDir,
        ignoreEntities);
  }

  /**
   * The position is specified in transform, and the size is specified in meta.
   *
   * @param parcelDir The parcel directory, which contains the {@value #META_FILE_NAME} file and
   *     {@value #DATA_DIR_NAME} directory. Will be created if not exists.
   * @throws IOException If an I/O error occurs while saving the parcel
   * @throws ParcelException If other error occurs while saving the parcel
   * @throws ParcelException.UnsupportedFormat If the format is not supported
   */
  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void save(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      @Nullable C config,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    ParcelFormat.Writer<C> format =
        (ParcelFormat.Writer<C>) ParcelFormatRegistry.get().getWriter(meta.formatSpec());
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
    }

    meta.save(getMetaFile(parcelDir));

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

    var space = new ParcelSpace(transform, meta.anchor());
    var source =
        new MinecraftParcelContentSource(
            level,
            meta.size(),
            meta.anchor(),
            space,
            ignoreEntities && meta.getExcludeEntities());
    format.write(
        new ParcelFormat.WriteContext<>(
            meta.size(),
            meta.anchor(),
            meta.dataVersion(),
            getDataDir(parcelDir),
            actualConfig),
        source);
  }

  public static <C extends ParcelFormatConfig<C>> void save(
      ParcelFormat.Writer<C> writer,
      Level level,
      BoundingBox boundingBox,
      Rotation rotation,
      Mirror mirror,
      @Nullable C config,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {

    var pivot = Parcel.getPivotBlockPos(mirror, rotation, boundingBox);
    ParcelTransform transform = new ParcelTransform(mirror, rotation, pivot);

    ParcelMeta meta = ParcelFactory.createMetadata(writer.spec(), boundingBox, rotation);

    ParcelStorage.save(level, transform, meta, config, parcelDir, ignoreEntities);
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
  public static <C extends ParcelFormatConfig<C>> void load(
      ServerLevel level,
      ParcelTransform transform,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags)
      throws IOException, ParcelException {
    var meta = ParcelMeta.load(parcelDir.resolve(META_FILE_NAME));
    ParcelFormat.Reader<C> reader =
        (ParcelFormat.Reader<C>) ParcelFormatRegistry.get().getReader(meta.formatSpec());
    if (reader == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
    }

    Path configFile = getConfigFile(parcelDir);
    C config = reader.getDefaultConfig();
    if (config != null && Files.exists(configFile)) {
      try {
        config.load(configFile);
      } catch (Exception e) {
        LOGGER.error(
            "Failed to load format config, use default and continue: {}", e.getMessage(), e);
      }
    }

    Path dataDir = parcelDir.resolve(DATA_DIR_NAME);
    var sink =
        new MinecraftParcelContentSink(
            level,
            new ParcelSpace(transform, meta.anchor()),
            ignoreBlocks,
            ignoreEntities,
            flags,
            meta.dataVersion());
    try {
      reader.read(
          new ParcelFormat.ReadContext<>(
              meta.size(),
              meta.anchor(),
              meta.dataVersion(),
              dataDir,
              config),
          sink);
    } finally {
      sink.finish();
    }
  }

  public static void load(
      ServerLevel level,
      BoundingBox boundingBox,
      Rotation rotation,
      Mirror mirror,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags)
      throws IOException, ParcelException {
    var pivot = Parcel.getPivotBlockPos(mirror, rotation, boundingBox);
    ParcelTransform transform = new ParcelTransform(mirror, rotation, pivot);
    load(level, transform, parcelDir, ignoreBlocks, ignoreEntities, flags);
  }
}
