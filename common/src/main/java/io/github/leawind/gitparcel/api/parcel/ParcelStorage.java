package io.github.leawind.gitparcel.api.parcel;

import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.world.Parcel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
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
    ParcelFormat.Saver<C> format = (ParcelFormat.Saver<C>) meta.getFormatSaver();
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
    }

    if (transform.rotation() != net.minecraft.world.level.block.Rotation.NONE
        && !format.features().contains(ParcelFormat.Feature.ROTATE)) {
      throw new ParcelException.UnsupportedFeature(meta.formatSpec(), ParcelFormat.Feature.ROTATE);
    }
    if (transform.mirror() != net.minecraft.world.level.block.Mirror.NONE
        && !format.features().contains(ParcelFormat.Feature.MIRROR)) {
      throw new ParcelException.UnsupportedFeature(meta.formatSpec(), ParcelFormat.Feature.MIRROR);
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

    format.save(
        level,
        meta.size(),
        meta.anchor(),
        transform,
        getDataDir(parcelDir),
        ignoreEntities && meta.getExcludeEntities(),
        actualConfig);
  }

  public static <C extends ParcelFormatConfig<C>> void save(
      ParcelFormat.Saver<C> saver,
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

    Vec3i sizeWorldSpace =
        new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
    Vec3i sizeParcelSpace = ParcelTransform.rotateSize(rotation, sizeWorldSpace);
    ParcelMeta meta = new ParcelMeta(saver.spec(), sizeParcelSpace, Vec3i.ZERO);

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
    ParcelFormat.Loader<C> loader = (ParcelFormat.Loader<C>) meta.getFormatLoader();
    if (loader == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
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
