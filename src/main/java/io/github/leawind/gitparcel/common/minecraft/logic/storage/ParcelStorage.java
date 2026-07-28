package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.content.ParcelContentTypeOrder;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelDataSink;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftParcelDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final String DATA_DIR_NAME = "data";

  private static Path getMetaFile(Path parcelDir) {
    return parcelDir.resolve(META_FILE_NAME);
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

  public static void save(
      Level level, Parcel parcel, Path parcelDir, boolean ignoreEntities)
      throws IOException, ParcelException {
    save(level, parcel, parcelDir, ignoreEntities, ProgressReporter.NONE);
  }

  public static void save(
      Level level,
      Parcel parcel,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    save(
        level,
        parcel.transform(),
        parcel.meta(),
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
   * @throws ParcelException.UnsupportedContent If a content implementation is unavailable
   */
  public static void save(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      Path parcelDir,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    save(
        level,
        transform,
        meta,
        parcelDir,
        ignoreEntities,
        ProgressReporter.NONE);
  }

  public static void save(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    Map<String, ParcelContentManifest> refreshedContents = refreshedContents(meta);
    replaceDirectory(
        parcelDir,
        stagingDir -> {
          if (Files.exists(parcelDir)) {
            NioFileTree.copyRecursively(parcelDir, stagingDir);
          }
          writeSnapshot(
              level,
              transform,
              meta,
              refreshedContents,
              stagingDir,
              ignoreEntities || meta.getExcludeEntities(),
              progress);
        });
    meta.setContents(refreshedContents);
  }

  private static void writeSnapshot(
      Level level,
      ParcelTransform transform,
      ParcelMeta meta,
      Map<String, ParcelContentManifest> contents,
      Path parcelDir,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    Map<String, ParcelContentManifest> previousContents = meta.contents();
    try {
      meta.setContents(contents);
      meta.save(getMetaFile(parcelDir));
    } finally {
      meta.setContents(previousContents);
    }

    var space = new ParcelSpace(transform, meta.anchor());
    var source =
        new MinecraftParcelDataSource(
            level, meta.size(), meta.anchor(), space, ignoreEntities);
    Path dataDirectory = getDataDir(parcelDir);
    var types = ParcelContentTypeOrder.forSave(ParcelContentTypeRegistry.get().latestTypes());
    for (ParcelContentType<?> type : types) {
      ParcelContentManifest manifest = contents.get(type.spec().id());
      saveContent(type, manifest, meta, dataDirectory.resolve(type.spec().id()), source, progress);
    }
    reconcileContentRoot(dataDirectory, contents.keySet());
    try (var entries = Files.newDirectoryStream(parcelDir)) {
      for (Path entry : entries) {
        String name = entry.getFileName().toString();
        if (!name.equals(META_FILE_NAME)
            && !name.equals(DATA_DIR_NAME)) {
          NioFileTree.deleteRecursivelyIfExists(entry);
        }
      }
    }
  }

  /** Updates an operation-owned NIO workspace into one complete snapshot. */
  public static void captureSnapshot(
      Level level,
      Parcel parcel,
      Path snapshotRoot,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    Files.createDirectories(snapshotRoot);
    Map<String, ParcelContentManifest> refreshedContents = refreshedContents(parcel.meta());
    writeSnapshot(
        level,
        parcel.transform(),
        parcel.meta(),
        refreshedContents,
        snapshotRoot,
        ignoreEntities || parcel.meta().getExcludeEntities(),
        progress);
    parcel.meta().setContents(refreshedContents);
  }

  private static Map<String, ParcelContentManifest> refreshedContents(ParcelMeta meta)
      throws ParcelException {
    for (var entry : meta.contents().entrySet()) {
      if (ParcelContentTypeRegistry.get().latest(entry.getKey()) == null) {
        throw new ParcelException.UnsupportedContent(entry.getValue().spec(entry.getKey()));
      }
    }
    var refreshed = new LinkedHashMap<String, ParcelContentManifest>();
    for (ParcelContentType<?> type : ParcelContentTypeRegistry.get().latestTypes()) {
      ParcelContentManifest previous = meta.contents().get(type.spec().id());
      var defaultConfig = type.defaultConfig();
      var config = previous == null ? null : previous.config();
      if (config == null && defaultConfig != null) {
        config = defaultConfig.toJson();
      }
      refreshed.put(
          type.spec().id(), new ParcelContentManifest(type.spec().version(), config));
    }
    return Map.copyOf(refreshed);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void saveContent(
      ParcelContentType type,
      ParcelContentManifest manifest,
      ParcelMeta meta,
      Path directory,
      ParcelDataSource source,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ParcelContentConfig config = parseConfig(type, manifest);
    type.save(
        new ParcelContentType.SaveContext<>(
            meta.size(), meta.anchor(), meta.dataVersion(), directory, config, progress),
        source);
  }

  private static ParcelContentConfig<?> parseConfig(
      ParcelContentType<?> type, ParcelContentManifest manifest) throws ParcelException {
    ParcelContentConfig<?> config = type.defaultConfig();
    var serialized = manifest.config();
    if (config == null) {
      if (serialized != null) {
        throw new ParcelException(
            "Parcel content type %s does not accept configuration".formatted(type.spec()));
      }
      return null;
    }
    if (serialized == null) {
      return config;
    }
    if (!serialized.isJsonObject()) {
      throw new ParcelException("Invalid configuration for parcel content " + type.spec());
    }
    try {
      config.setFromJson(serialized.getAsJsonObject());
    } catch (IllegalStateException e) {
      throw new ParcelException("Invalid configuration for parcel content " + type.spec(), e);
    }
    return config;
  }

  private static void reconcileContentRoot(Path dataRoot, Set<String> contentIds)
      throws IOException {
    Files.createDirectories(dataRoot);
    try (var entries = Files.newDirectoryStream(dataRoot)) {
      for (Path entry : entries) {
        if (!contentIds.contains(entry.getFileName().toString())) {
          NioFileTree.deleteRecursivelyIfExists(entry);
        }
      }
    }
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
   * @throws ParcelException.UnsupportedContent If a content implementation is unavailable
   */
  public static void load(
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
  public static ParcelMeta validateSnapshot(
      Path parcelDir, ProgressReporter progress) throws IOException, ParcelException {
    ParcelMeta meta = ParcelMeta.load(getMetaFile(parcelDir));
    Path dataDir = getDataDir(parcelDir);
    if (!Files.isDirectory(dataDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Snapshot data directory not found: " + dataDir);
    }

    var types = resolveSnapshotContentTypes(meta, dataDir);
    var sink = new SnapshotValidationSink(meta);
    for (ParcelContentType<?> type : ParcelContentTypeOrder.forLoad(types)) {
      loadContent(
          type,
          meta.contents().get(type.spec().id()),
          meta,
          dataDir.resolve(type.spec().id()),
          sink,
          ProgressReporter.prefixed("validate_", progress));
    }
    sink.finish();
    return meta;
  }

  public static void load(
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
  public static void applyValidatedSnapshot(
      ServerLevel level,
      ParcelTransform transform,
      Path parcelDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      ProgressReporter progress)
      throws IOException, ParcelException {
    var meta = ParcelMeta.load(parcelDir.resolve(META_FILE_NAME));
    Path dataDir = parcelDir.resolve(DATA_DIR_NAME);
    var types = resolveSnapshotContentTypes(meta, dataDir);
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
        new MinecraftParcelDataSink(
            level,
            space,
            ignoreBlocks,
            ignoreEntities,
            flags,
            meta.dataVersion());
    try {
      for (ParcelContentType<?> type : ParcelContentTypeOrder.forLoad(types)) {
        loadContent(
            type,
            meta.contents().get(type.spec().id()),
            meta,
            dataDir.resolve(type.spec().id()),
            sink,
            progress);
      }
    } finally {
      sink.finish();
    }
  }

  private static java.util.List<ParcelContentType<?>> resolveSnapshotContentTypes(
      ParcelMeta meta, Path dataDir) throws IOException, ParcelException {
    validateContentRoot(dataDir, meta.contents().keySet());
    var types = new java.util.ArrayList<ParcelContentType<?>>(meta.contents().size());
    for (var entry : meta.contents().entrySet()) {
      ParcelContentType.Spec spec = entry.getValue().spec(entry.getKey());
      ParcelContentType<?> type = ParcelContentTypeRegistry.get().get(spec);
      if (type == null) {
        throw new ParcelException.UnsupportedContent(spec);
      }
      types.add(type);
    }
    return List.copyOf(types);
  }

  private static void validateContentRoot(Path dataRoot, Set<String> contentIds)
      throws IOException, ParcelException.CorruptedParcelException {
    try (var entries = Files.newDirectoryStream(dataRoot)) {
      var found = new java.util.HashSet<String>();
      for (Path entry : entries) {
        String id = entry.getFileName().toString();
        if (!contentIds.contains(id) || !Files.isDirectory(entry)) {
          throw new ParcelException.CorruptedParcelException(
              "Unexpected snapshot content entry: " + entry);
        }
        found.add(id);
      }
      if (!found.equals(contentIds)) {
        var missing = new java.util.HashSet<>(contentIds);
        missing.removeAll(found);
        throw new ParcelException.CorruptedParcelException(
            "Missing snapshot content directories: " + missing);
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void loadContent(
      ParcelContentType type,
      ParcelContentManifest manifest,
      ParcelMeta meta,
      Path directory,
      ParcelDataSink sink,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ParcelContentConfig config;
    try {
      config = parseConfig(type, manifest);
    } catch (ParcelException e) {
      throw new ParcelException.CorruptedParcelException(e.getMessage(), e);
    }
    type.load(
        new ParcelContentType.LoadContext<>(
            meta.size(), meta.anchor(), meta.dataVersion(), directory, config, progress),
        sink);
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

  private static final class SnapshotValidationSink implements ParcelDataSink {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxXExclusive;
    private final int maxYExclusive;
    private final int maxZExclusive;

    private SnapshotValidationSink(ParcelMeta meta) {
      minX = -meta.anchor().getX();
      minY = -meta.anchor().getY();
      minZ = -meta.anchor().getZ();
      maxXExclusive = Math.addExact(minX, meta.size().getX());
      maxYExclusive = Math.addExact(minY, meta.size().getY());
      maxZExclusive = Math.addExact(minZ, meta.size().getZ());
    }

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
      var origin = section.origin();
      int endX = Math.addExact(origin.getX(), section.size().getX());
      int endY = Math.addExact(origin.getY(), section.size().getY());
      int endZ = Math.addExact(origin.getZ(), section.size().getZ());
      if (!contains(origin.getX(), origin.getY(), origin.getZ())
          || endX > maxXExclusive
          || endY > maxYExclusive
          || endZ > maxZExclusive) {
        throw new ParcelException.CorruptedParcelException(
            "Block section lies outside the parcel: " + section.origin());
      }
      for (var blockEntity : section.blockEntities()) {
        var pos = blockEntity.pos();
        if (pos.getX() < origin.getX()
            || pos.getY() < origin.getY()
            || pos.getZ() < origin.getZ()
            || pos.getX() >= endX
            || pos.getY() >= endY
            || pos.getZ() >= endZ) {
          throw new ParcelException.CorruptedParcelException(
              "Block entity lies outside its section: " + pos);
        }
        validateSemanticData(blockEntity.semanticData());
      }
    }

    @Override
    public void acceptEntity(EntityRecord entity) throws ParcelException {
      var pos = entity.pos();
      if (!Double.isFinite(pos.x)
          || !Double.isFinite(pos.y)
          || !Double.isFinite(pos.z)
          || pos.x < minX
          || pos.y < minY
          || pos.z < minZ
          || pos.x >= maxXExclusive
          || pos.y >= maxYExclusive
          || pos.z >= maxZExclusive) {
        throw new ParcelException.CorruptedParcelException(
            "Entity lies outside the parcel: " + pos);
      }
      validateSemanticData(entity.semanticData());
    }

    private boolean contains(int x, int y, int z) {
      return x >= minX
          && y >= minY
          && z >= minZ
          && x < maxXExclusive
          && y < maxYExclusive
          && z < maxZExclusive;
    }

    private static void validateSemanticData(java.util.List<SemanticData> semantics)
        throws ParcelException {
      for (var semantic : semantics) {
        if (ParcelRecordProcessorRegistry.get().get(semantic.processor()) == null) {
          throw new ParcelException(
              "Missing required parcel data processor: " + semantic.processor());
        }
      }
    }
  }
}
