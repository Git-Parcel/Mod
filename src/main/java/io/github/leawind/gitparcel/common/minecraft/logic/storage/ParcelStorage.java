package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessorRegistry;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSink;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelContentSink;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelContentSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

  /** A path within a shared repository working tree. Internal snapshots do not use this type. */
  public record RepositoryLocation(
      Path repository,
      Path relative,
      @Nullable String sharedRepository) {
    public RepositoryLocation(Path repository, Path relative) {
      this(repository, relative, null);
    }

    public RepositoryLocation {
      repository = repository.normalize();
      relative = relative.normalize();
      if (relative.isAbsolute()
          || relative.toString().isEmpty()
          || relative.startsWith("..")) {
        throw new IllegalArgumentException("Parcel path must stay below the repository");
      }
      for (Path part : relative) {
        if (part.toString().equals(".git")) {
          throw new IllegalArgumentException("Parcel path must not contain .git");
        }
      }
    }

    public Path parcelDirectory() {
      return repository.resolve(relative).normalize();
    }

    /** Returns the repository-relative path using Git's platform-independent separator. */
    public String gitPath() {
      return StreamSupport.stream(relative.spliterator(), false)
          .map(Path::toString)
          .collect(Collectors.joining("/"));
    }
  }

  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void save(
      Level level, Parcel parcel, Path parcelDir, boolean ignoreEntities)
      throws IOException, ParcelException {
    save(level, parcel, parcelDir, ignoreEntities, ProgressReporter.NONE);
  }

  public static <C extends ParcelFormatConfig<C>> void save(
      Level level,
      Parcel parcel,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
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
        ignoreEntities,
        progress);
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
    save(
        level,
        transform,
        meta,
        config,
        parcelDir,
        ignoreEntities,
        ProgressReporter.NONE);
  }

  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void save(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      @Nullable C config,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ParcelFormat.Writer<C> format =
        (ParcelFormat.Writer<C>) ParcelFormatRegistry.get().getWriter(meta.formatSpec());
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
    }

    C actualConfig = config;
    if (actualConfig == null) {
      actualConfig = format.getDefaultConfig();
      var existingConfigFile = getConfigFile(parcelDir);
      if (actualConfig != null && Files.exists(existingConfigFile)) {
        try {
          actualConfig.load(existingConfigFile);
        } catch (Exception e) {
          LOGGER.error(
              "Failed to load format config, use default and overwrite: {}", e.getMessage(), e);
          actualConfig.resetToDefault();
        }
      }
    }

    C resolvedConfig = actualConfig;
    replaceDirectory(
        parcelDir,
        stagingDir ->
            writeSnapshot(
                format,
                level,
                transform,
                meta,
                resolvedConfig,
                stagingDir,
                ignoreEntities || meta.getExcludeEntities(),
                progress));
  }

  private static <C extends ParcelFormatConfig<C>> void writeSnapshot(
      ParcelFormat.Writer<C> format,
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      @Nullable C config,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    meta.save(getMetaFile(parcelDir));
    if (config != null) {
      config.save(getConfigFile(parcelDir));
    }

    var space = new ParcelSpace(transform, meta.anchor());
    var source =
        new MinecraftParcelContentSource(
            level, meta.size(), meta.anchor(), space, ignoreEntities);
    format.write(
        new ParcelFormat.WriteContext<>(
            meta.size(),
            meta.anchor(),
            meta.dataVersion(),
            getDataDir(parcelDir),
            config,
            progress),
        source);
  }

  /** Writes one complete snapshot into an operation-owned empty NIO workspace. */
  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void captureSnapshot(
      Level level,
      Parcel parcel,
      Path snapshotRoot,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    if (Files.exists(snapshotRoot)) {
      try (var entries = Files.list(snapshotRoot)) {
        if (entries.findAny().isPresent()) {
          throw new IOException("Snapshot workspace must be empty: " + snapshotRoot);
        }
      }
    }
    Files.createDirectories(snapshotRoot);
    ParcelFormat.Writer<C> format =
        (ParcelFormat.Writer<C>)
            ParcelFormatRegistry.get().getWriter(parcel.meta().formatSpec());
    if (format == null) {
      throw new ParcelException.UnsupportedFormat(parcel.meta().formatSpec());
    }
    C config = format.getDefaultConfig();
    var serialized = parcel.formatConfig().orElse(null);
    if (config != null && serialized != null) {
      config.setFromJson(serialized.getAsJsonObject());
    }
    writeSnapshot(
        format,
        level,
        parcel.transform(),
        parcel.meta(),
        config,
        snapshotRoot,
        ignoreEntities || parcel.meta().getExcludeEntities(),
        progress);
  }

  @FunctionalInterface
  interface DirectoryWriter {
    void write(Path directory) throws IOException, ParcelException;
  }

  /**
   * Builds a complete replacement next to the destination before swapping it into place.
   *
   * <p>If writing fails, the existing directory is left untouched. If installation fails after the
   * existing directory has been moved aside, restoration is attempted before the failure is
   * propagated.
   */
  static void replaceDirectory(Path target, DirectoryWriter writer)
      throws IOException, ParcelException {
    Path normalizedTarget = target.normalize();
    Path parent = normalizedTarget.getParent();
    if (parent == null) {
      normalizedTarget = normalizedTarget.toAbsolutePath().normalize();
      parent = normalizedTarget.getParent();
    }
    Files.createDirectories(parent);

    String fileName =
        normalizedTarget.getFileName() == null
            ? "parcel"
            : normalizedTarget.getFileName().toString();
    Path staging = Files.createTempDirectory(parent, "." + fileName + ".staging-");
    Path backup =
        parent.resolve("." + fileName + ".backup-" + UUID.randomUUID());
    boolean previousMoved = false;
    boolean installed = false;

    try {
      writer.write(staging);

      if (Files.exists(normalizedTarget)) {
        move(normalizedTarget, backup);
        previousMoved = true;
      }

      try {
        move(staging, normalizedTarget);
        installed = true;
      } catch (IOException installFailure) {
        if (previousMoved) {
          try {
            move(backup, normalizedTarget);
            previousMoved = false;
          } catch (IOException restoreFailure) {
            installFailure.addSuppressed(restoreFailure);
          }
        }
        throw installFailure;
      }

      if (previousMoved) {
        try {
          deleteRecursively(backup);
          previousMoved = false;
        } catch (IOException cleanupFailure) {
          LOGGER.warn("Failed to remove parcel backup {}", backup, cleanupFailure);
        }
      }
    } finally {
      if (!installed) {
        deleteRecursivelyIfExists(staging);
      }
    }
  }

  private static void move(Path source, Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
      Files.move(source, target);
    }
  }

  /** Deletes a snapshot tree when cleaning up a temporary or rolled-back operation. */
  public static void deleteRecursivelyIfExists(Path directory) throws IOException {
    NioFileTree.deleteRecursivelyIfExists(directory);
  }

  private static void deleteRecursively(Path directory) throws IOException {
    NioFileTree.deleteRecursivelyIfExists(directory);
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
    load(
        level,
        transform,
        parcelDir,
        ignoreBlocks,
        ignoreEntities,
        flags,
        ProgressReporter.NONE);
  }

  /** Fully decodes a portable snapshot without touching a world. */
  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> ParcelMeta validateSnapshot(
      Path parcelDir, ProgressReporter progress) throws IOException, ParcelException {
    ParcelMeta meta = ParcelMeta.load(getMetaFile(parcelDir));
    ParcelFormat.Reader<C> reader =
        (ParcelFormat.Reader<C>) ParcelFormatRegistry.get().getReader(meta.formatSpec());
    if (reader == null) {
      throw new ParcelException.UnsupportedFormat(meta.formatSpec());
    }
    Path dataDir = getDataDir(parcelDir);
    if (!Files.isDirectory(dataDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Snapshot data directory not found: " + dataDir);
    }

    C config = reader.getDefaultConfig();
    Path configFile = getConfigFile(parcelDir);
    if (Files.exists(configFile)) {
      if (config == null) {
        throw new ParcelException.CorruptedParcelException(
            "Snapshot has configuration for a format that does not accept it");
      }
      try {
        config.load(configFile);
      } catch (Exception e) {
        throw new ParcelException.CorruptedParcelException(
            "Invalid snapshot format configuration", e);
      }
    }

    reader.read(
        new ParcelFormat.ReadContext<>(
            meta.size(),
            meta.anchor(),
            meta.dataVersion(),
            dataDir,
            config,
            ProgressReporter.prefixed("validate_", progress)),
        new SnapshotValidationSink());
    return meta;
  }

  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void load(
      ServerLevel level,
      ParcelTransform transform,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      ProgressReporter progress)
      throws IOException, ParcelException {
    validateSnapshot(parcelDir, progress);
    applyValidatedSnapshot(
        level,
        transform,
        parcelDir,
        ignoreBlocks,
        ignoreEntities,
        flags,
        progress);
  }

  /** Applies a snapshot whose complete portable tree was already decoded and validated. */
  @SuppressWarnings("unchecked")
  public static <C extends ParcelFormatConfig<C>> void applyValidatedSnapshot(
      ServerLevel level,
      ParcelTransform transform,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      ProgressReporter progress)
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
    var space = new ParcelSpace(transform, meta.anchor());
    if (!ignoreEntities) {
      level
          .getEntities(
              (Entity) null,
              worldBounds(space, meta.size(), meta.anchor()),
              entity -> !(entity instanceof Player))
          .forEach(Entity::discard);
    }
    var sink =
        new MinecraftParcelContentSink(
            level,
            space,
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
              config,
              progress),
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

  private static AABB worldBounds(ParcelSpace space, net.minecraft.core.Vec3i size, net.minecraft.core.Vec3i anchor) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;
    int[] xs = {-anchor.getX(), size.getX() - anchor.getX()};
    int[] ys = {-anchor.getY(), size.getY() - anchor.getY()};
    int[] zs = {-anchor.getZ(), size.getZ() - anchor.getZ()};
    for (int x : xs) {
      for (int y : ys) {
        for (int z : zs) {
          Vec3 point = space.toWorld(new Vec3(x, y, z));
          minX = Math.min(minX, point.x);
          minY = Math.min(minY, point.y);
          minZ = Math.min(minZ, point.z);
          maxX = Math.max(maxX, point.x);
          maxY = Math.max(maxY, point.y);
          maxZ = Math.max(maxZ, point.z);
        }
      }
    }
    return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
  }

  private static final class SnapshotValidationSink implements ParcelContentSink {
    @Override
    public void acceptAttachment(AttachmentRecord attachment) throws ParcelException {
      var type = ParcelAttachmentTypeRegistry.get().get(attachment.type());
      if (type == null) {
        if (attachment.required()) {
          throw new ParcelException(
              "Missing required parcel attachment type: " + attachment.type());
        }
        return;
      }
      if (type.schemaVersion() != attachment.schemaVersion()) {
        throw new ParcelException(
            "Unsupported schema version %d for attachment %s"
                .formatted(attachment.schemaVersion(), attachment.type()));
      }
    }

    @Override
    public void acceptBlockSection(BlockSection section) throws ParcelException {
      for (var blockEntity : section.blockEntities()) {
        validateSemanticData(blockEntity.semanticData());
      }
    }

    @Override
    public void acceptEntity(EntityRecord entity) throws ParcelException {
      validateSemanticData(entity.semanticData());
    }

    private static void validateSemanticData(java.util.List<SemanticData> semantics)
        throws ParcelException {
      for (var semantic : semantics) {
        if (ParcelDataProcessorRegistry.get().get(semantic.processor()) == null) {
          throw new ParcelException(
              "Missing required parcel data processor: " + semantic.processor());
        }
      }
    }
  }
}
