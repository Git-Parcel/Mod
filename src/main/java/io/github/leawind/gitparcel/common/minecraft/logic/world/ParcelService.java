package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.impl.world.ParcelValidator;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelRepositoryService;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.common.utils.git.InternalRepository;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedContent;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Coordinates authoritative parcel state, internal snapshots, and explicit content exchange. */
public final class ParcelService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ParcelService.class);
  private static final ConcurrentHashMap<UUID, ReentrantLock> PARCEL_LOCKS =
      new ConcurrentHashMap<>();
  private static final int LOAD_FLAGS =
      Block.UPDATE_CLIENTS
          | Block.UPDATE_IMMEDIATE
          | Block.UPDATE_KNOWN_SHAPE
          | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;

  private final ServerLevel level;
  private final GitParcelLevelSavedData savedData;

  private ParcelService(ServerLevel level) {
    this.level = level;
    this.savedData = GitParcelLevelSavedData.get(level);
    String dimension = level.dimension().identifier().toString();
    boolean changed = false;
    for (Parcel parcel : savedData.parcels().values()) {
      if (parcel.dimension().isEmpty()) {
        parcel.assignDimension(dimension);
        changed = true;
      }
    }
    if (changed) {
      savedData.setDirty();
    }
  }

  public static ParcelService get(ServerLevel level) {
    return new ParcelService(level);
  }

  public Collection<Parcel> parcels() {
    return List.copyOf(savedData.parcels().values());
  }

  public @Nullable Parcel getParcel(UUID uuid) {
    return savedData.parcels().get(uuid);
  }

  public void reset() {
    savedData.clearParcels();
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.fullSync(savedData.parcels()));
  }

  public void addNewParcel(Parcel parcel) throws IllegalArgumentException {
    parcel.assignDimension(level.dimension().identifier().toString());
    validateNewParcel(parcel);
    savedData.addParcel(parcel);
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  public void validateNewParcel(Parcel parcel) throws IllegalArgumentException {
    var maxParcelVolume = GitParcelWorldSavedData.get(level.getServer()).maxParcelVolume();
    ParcelValidator.validateNewParcel(parcel, savedData.parcels().values(), maxParcelVolume);
  }

  /** Removes only the world registration; the repository is deliberately retained. */
  public @Nullable Parcel deleteParcel(UUID uuid) throws ParcelException {
    ReentrantLock lock = acquireParcelLock(uuid);
    try {
      var deleted = savedData.removeParcel(uuid);
      if (deleted != null) {
        Services.SERVER_NETWORKING.sendToAllPlayers(
            level, UpdateParcelsMessage.removals(Set.of(uuid)));
      }
      return deleted;
    } finally {
      lock.unlock();
    }
  }

  /** Atomically validates and unregisters a batch while acquiring locks in UUID order. */
  public int deleteParcels(Collection<Parcel> parcels) throws ParcelException {
    var targets =
        parcels.stream()
            .distinct()
            .sorted(Comparator.comparing(parcel -> parcel.uuid().toString()))
            .toList();
    var locks = new ArrayList<ReentrantLock>(targets.size());
    try {
      for (Parcel parcel : targets) {
        locks.add(acquireParcelLock(parcel.uuid()));
      }
      for (Parcel parcel : targets) {
        requireRegistered(parcel);
      }
      var removed = new java.util.HashSet<UUID>();
      for (Parcel parcel : targets) {
        if (savedData.removeParcel(parcel.uuid()) != null) {
          removed.add(parcel.uuid());
        }
      }
      if (!removed.isEmpty()) {
        Services.SERVER_NETWORKING.sendToAllPlayers(
            level, UpdateParcelsMessage.removals(Set.copyOf(removed)));
      }
      return removed.size();
    } finally {
      for (int i = locks.size() - 1; i >= 0; i--) {
        locks.get(i).unlock();
      }
    }
  }

  /** Marks a mutated parcel as dirty and synchronizes runtime properties to clients. */
  public void updateParcel(Parcel parcel) {
    requireRegistered(parcel);
    savedData.setDirty();
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  /**
   * Server-thread entry point: performs blocking world capture on the calling thread. Intended for
   * GameTests; production commands must use {@link #saveSnapshotInBackground}.
   */
  @ApiStatus.Internal
  public SnapshotId saveSnapshot(
      Parcel parcel,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    return saveSnapshot(
        parcel,
        name,
        description,
        identity,
        ignoreEntities,
        ProgressReporter.NONE);
  }

  /**
   * Server-thread entry point: performs blocking world capture on the calling thread. Intended for
   * GameTests; production commands must use {@link #saveSnapshotInBackground}.
   */
  @ApiStatus.Internal
  public SnapshotId saveSnapshot(
      Parcel parcel,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      requireRegistered(parcel);
      return ParcelRepositoryService.saveSnapshot(
          level,
          parcel,
          internalParcelsDirectory(),
          name,
          description,
          identity,
          ignoreEntities,
          TemporarySnapshotWorkspaceFactory.INSTANCE,
          progress,
          SnapshotNode.Source.SAVED);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Server-thread entry point: blocks on the repository lock while acquiring the tree page.
   * Intended for GameTests; production queries must use {@link #querySnapshotTreeInBackground}.
   */
  @ApiStatus.Internal
  public SnapshotTreePage querySnapshotTree(
      Parcel parcel, int limit, Optional<SnapshotId> cursor) throws IOException {
    requireRegistered(parcel);
    return ParcelRepositoryService.querySnapshotTree(
        parcel, internalParcelsDirectory(), limit, cursor);
  }

  /** Reads snapshot history off-thread after rechecking registration on the server thread. */
  public SnapshotTreePage querySnapshotTreeInBackground(
      Parcel parcel,
      int limit,
      Optional<SnapshotId> cursor,
      ServerThreadBridge serverThread)
      throws Exception {
    serverThread.run(() -> requireRegistered(parcel));
    return ParcelRepositoryService.querySnapshotTree(
        parcel, internalParcelsDirectory(), limit, cursor);
  }

  /**
   * Server-thread entry point: writes the world synchronously on the calling thread. Intended for
   * GameTests; production commands must use {@link #restoreSnapshotInBackground}.
   */
  @ApiStatus.Internal
  public InternalRepository.RestoreResult restoreSnapshot(
      Parcel parcel,
      SnapshotId snapshotId,
      RestoreSnapshotRequest.Mode mode,
      boolean ignoreEntities,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    return restoreSnapshot(
        parcel,
        snapshotId,
        mode,
        ignoreEntities,
        identity,
        ProgressReporter.NONE);
  }

  /**
   * Server-thread entry point: writes the world synchronously on the calling thread. Intended for
   * GameTests; production commands must use {@link #restoreSnapshotInBackground}.
   */
  @ApiStatus.Internal
  public InternalRepository.RestoreResult restoreSnapshot(
      Parcel parcel,
      SnapshotId snapshotId,
      RestoreSnapshotRequest.Mode mode,
      boolean ignoreEntities,
      GitRepo.CommitIdentity identity,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      requireRegistered(parcel);
      if (mode == RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE) {
        saveSnapshot(
            parcel,
            "Before restore " + snapshotId.abbreviate(),
            "Automatically requested before restoring another snapshot.",
            identity,
            ignoreEntities,
            progress);
      }
      return ParcelRepositoryService.restoreSnapshot(
          level,
          parcel,
          internalParcelsDirectory(),
          snapshotId,
          ignoreEntities,
          TemporarySnapshotWorkspaceFactory.INSTANCE,
          progress);
    } finally {
      lock.unlock();
    }
  }

  /** Server-thread entry point for listing durable restore records. */
  @ApiStatus.Internal
  public InternalRepository.PendingRestoreReport pendingRestoreOperations(Parcel parcel)
      throws IOException {
    requireRegistered(parcel);
    return internalRepository(parcel).pendingRestores();
  }

  /** Resolves a durable restore record synchronously. Prefer the background entry point in-game. */
  @ApiStatus.Internal
  public InternalRepository.RestoreResult resolvePendingRestore(
      Parcel parcel, UUID operationId, boolean rollback, boolean ignoreEntities)
      throws IOException, ParcelException {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      requireRegistered(parcel);
      return ParcelRepositoryService.resolvePendingRestore(
          level,
          parcel,
          internalParcelsDirectory(),
          operationId,
          rollback,
          ignoreEntities,
          TemporarySnapshotWorkspaceFactory.INSTANCE,
          ProgressReporter.NONE,
          ServerThreadBridge.DIRECT);
    } finally {
      lock.unlock();
    }
  }

  /** Background save orchestration: only world capture crosses back to the server thread. */
  public SnapshotId saveSnapshotInBackground(
      Parcel parcel,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      Path snapshotRoot = workspace.root().resolve("snapshot");
      Optional<SnapshotId> baseline =
          ParcelRepositoryService.prepareSnapshotWorkspace(
              parcel, internalParcelsDirectory(), snapshotRoot, progress);
      serverThread.run(
          () -> {
            requireRegistered(parcel);
            ParcelRepositoryService.captureSnapshot(
                level, parcel, snapshotRoot, ignoreEntities, progress);
          });
      return ParcelRepositoryService.saveWorkspaceSnapshot(
          parcel,
          internalParcelsDirectory(),
          snapshotRoot,
          name,
          description,
          identity,
          SnapshotNode.Source.SAVED,
          progress,
          baseline);
    } finally {
      lock.unlock();
    }
  }

  /** Background restore orchestration with Git and content validation off the server thread. */
  public InternalRepository.RestoreResult restoreSnapshotInBackground(
      Parcel parcel,
      SnapshotId snapshotId,
      RestoreSnapshotRequest.Mode mode,
      boolean ignoreEntities,
      GitRepo.CommitIdentity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      serverThread.run(() -> requireRegistered(parcel));
      if (mode == RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE) {
        try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
          Path snapshotRoot = workspace.root().resolve("snapshot");
          Optional<SnapshotId> baseline =
              ParcelRepositoryService.prepareSnapshotWorkspace(
                  parcel, internalParcelsDirectory(), snapshotRoot, progress);
          serverThread.run(
              () ->
                  ParcelRepositoryService.captureSnapshot(
                      level, parcel, snapshotRoot, ignoreEntities, progress));
          ParcelRepositoryService.saveWorkspaceSnapshot(
              parcel,
              internalParcelsDirectory(),
              snapshotRoot,
              "Before restore " + snapshotId.abbreviate(),
              "Automatically requested before restoring another snapshot.",
              identity,
              SnapshotNode.Source.SAVED,
              progress,
              baseline);
        }
      }
      return ParcelRepositoryService.restoreSnapshot(
          level,
          parcel,
          internalParcelsDirectory(),
          snapshotId,
          ignoreEntities,
          TemporarySnapshotWorkspaceFactory.INSTANCE,
          progress,
          serverThread);
    } finally {
      lock.unlock();
    }
  }

  /** Retries or rolls back a durable restore record with filesystem work off-thread. */
  public InternalRepository.RestoreResult resolvePendingRestoreInBackground(
      Parcel parcel,
      UUID operationId,
      boolean rollback,
      boolean ignoreEntities,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      serverThread.run(() -> requireRegistered(parcel));
      return ParcelRepositoryService.resolvePendingRestore(
          level,
          parcel,
          internalParcelsDirectory(),
          operationId,
          rollback,
          ignoreEntities,
          TemporarySnapshotWorkspaceFactory.INSTANCE,
          progress,
          serverThread);
    } finally {
      lock.unlock();
    }
  }

  /** Audits repositories without rewriting them and reports parcels requiring operator recovery. */
  public void auditRepositories(Collection<Parcel> parcels) {
    for (Parcel parcel : parcels) {
      var repository = internalRepository(parcel);
      if (!repository.exists()) {
        continue;
      }
      var state = repository.inspect();
      if (state.health() == InternalRepository.Health.READ_ONLY) {
        LOGGER.error(
            "Parcel {} repository entered read-only fault state: {}",
            parcel.uuid(),
            String.join("; ", state.diagnostics()));
        continue;
      }
      try {
        var pending = repository.pendingRestores();
        for (var problem : pending.diagnostics()) {
          LOGGER.error(
              "Parcel {} has an unreadable restore operation record: {}", parcel.uuid(), problem);
        }
        if (!pending.operations().isEmpty()) {
          LOGGER.warn(
              "Parcel {} has {} unfinished restore operation(s) and requires recovery",
              parcel.uuid(),
              pending.operations().size());
        }
      } catch (IOException e) {
        LOGGER.error("Failed to inspect restore operations for parcel {}", parcel.uuid(), e);
      }
    }
  }

  /** Server-thread entry point. Prefer {@link #publishCurrentSnapshotInBackground} in-game. */
  @ApiStatus.Internal
  public Optional<GitRepo.CommitInfo> publishSnapshot(
      Parcel parcel,
      SnapshotId snapshotId,
      String repository,
      String parcelPath,
      String message,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    requireRegistered(parcel);
    return publishSnapshotUnchecked(
        parcel,
        snapshotId,
        repository,
        parcelPath,
        message,
        identity,
        ProgressReporter.NONE);
  }

  private Optional<GitRepo.CommitInfo> publishSnapshotUnchecked(
      Parcel parcel,
      SnapshotId snapshotId,
      String repository,
      String parcelPath,
      String message,
      GitRepo.CommitIdentity identity,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ProgressReporter reporter = ProgressReporter.safe(progress);
    internalRepository(parcel).current(); // also performs the repository health check

    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      Path exported = workspace.root().resolve("snapshot");
      internalRepository(parcel).exportSnapshot(snapshotId, exported, reporter);
      try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
        String gitPath = lease.content().getParcelGitPath(repository, parcelPath);
        Path relative = lease.repository().getFileSystem().getPath(gitPath);
        var location =
            new ParcelStorage.RepositoryLocation(lease.repository(), relative, repository);
        if (lease.content().containsParcelPath(repository, gitPath)
            || Files.exists(location.parcelDirectory())) {
          throw new ParcelException("Shared parcel path already exists: " + gitPath);
        }

        var manifestSnapshot = lease.content().snapshotRepoMeta(repository);
        try {
          copySnapshot(exported, location.parcelDirectory(), reporter);
          lease.content().addParcelPath(repository, gitPath);
          GitRepo.CommitInfo result =
              GitRepo.get(location.repository())
                  .commit(
                      List.of(location.gitPath(), SharedContent.REPOSITORY_MANIFEST_FILE),
                      message,
                      identity)
                  .orElseThrow(
                      () -> new IOException("Publishing produced no shared repository commit"));
          return Optional.of(result);
        } catch (IOException | GitAPIException | RuntimeException e) {
          rollbackPublish(lease, repository, location, manifestSnapshot, e);
          if (e instanceof GitAPIException) {
            throw new ParcelException("Failed to commit published snapshot", e);
          }
          if (e instanceof IOException io) {
            throw io;
          }
          throw (RuntimeException) e;
        }
      }
    }
  }

  /** Server-thread entry point: publishes the current baseline, if one exists. */
  @ApiStatus.Internal
  public Optional<GitRepo.CommitInfo> publishCurrentSnapshot(
      Parcel parcel,
      String repository,
      String parcelPath,
      String message,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    SnapshotId current =
        internalRepository(parcel)
            .current()
            .orElseThrow(() -> new ParcelException("Parcel has no saved snapshot to publish"));
    return publishSnapshot(parcel, current, repository, parcelPath, message, identity);
  }

  /** Publishes the current saved baseline with all repository I/O on the calling worker. */
  public GitRepo.CommitInfo publishCurrentSnapshotInBackground(
      Parcel parcel,
      String repository,
      String parcelPath,
      String message,
      GitRepo.CommitIdentity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    serverThread.run(() -> requireRegistered(parcel));
    SnapshotId current =
        internalRepository(parcel)
            .current()
            .orElseThrow(() -> new ParcelException("Parcel has no saved snapshot to publish"));
    return publishSnapshotUnchecked(
            parcel, current, repository, parcelPath, message, identity, progress)
        .orElseThrow(() -> new IOException("Publishing produced no shared repository commit"));
  }

  /** Server-thread entry point: applies imported content to the world on the calling thread. */
  @ApiStatus.Internal
  public Parcel importSharedSnapshot(
      String repository,
      String revision,
      String parcelPath,
      ParcelTransform transform,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    try {
      return importSharedSnapshotInBackground(
          repository,
          revision,
          parcelPath,
          transform,
          identity,
          ProgressReporter.NONE,
          ServerThreadBridge.DIRECT);
    } catch (IOException | ParcelException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to import shared snapshot", e);
    }
  }

  /** Performs shared Git and content I/O off-thread, crossing over only for world state. */
  public Parcel importSharedSnapshotInBackground(
      String repository,
      String revision,
      String parcelPath,
      ParcelTransform transform,
      GitRepo.CommitIdentity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      final String gitPath;
      final SnapshotId sharedCommit;
      Path snapshot = workspace.root().resolve("snapshot");
      try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
        gitPath = lease.content().getParcelGitPath(repository, parcelPath);
        sharedCommit = GitRepo.get(lease.repository()).core().resolveCommit(revision);
        if (!SharedRepositoryService.get(level.getServer())
            .parcelPaths(repository, sharedCommit.value())
            .contains(gitPath)) {
          throw new ParcelException("Parcel path is not listed by shared repository: " + gitPath);
        }
        GitRepo.get(lease.repository()).exportRevision(sharedCommit.value(), gitPath, snapshot);
      }
      ParcelMeta meta = ParcelStorage.validateSnapshot(snapshot, progress);
      var parcel =
          serverThread.call(
              () -> {
                var permissions =
                    GitParcelWorldSavedData.get(level.getServer())
                        .parcelDefaultPermissions()
                        .copy();
                return Parcel.create(meta, transform, permissions);
              });

      ParcelRepositoryService.saveWorkspaceSnapshot(
          parcel,
          internalParcelsDirectory(),
          snapshot,
          "Imported snapshot",
          "Imported from %s@%s:%s".formatted(repository, sharedCommit.value(), gitPath),
          identity,
          SnapshotNode.Source.IMPORTED,
          progress);
      serverThread.run(
          () -> {
            validateNewParcel(parcel);
            ParcelStorage.applyValidatedSnapshot(
                level,
                transform,
                snapshot,
                false,
                meta.getExcludeEntities(),
                LOAD_FLAGS,
                ProgressReporter.prefixed("import_", progress));
            addNewParcel(parcel);
          });
      return parcel;
    }
  }

  /**
   * Imports external content as a new child snapshot without implicitly writing the live world.
   * Performs repository I/O on the calling thread; schedule it on a worker before using in-game.
   */
  @ApiStatus.Internal
  public SnapshotId importSharedSnapshotIntoParcel(
      Parcel parcel,
      String repository,
      String revision,
      String parcelPath,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    ReentrantLock lock = acquireParcelLock(parcel.uuid());
    try {
      requireRegistered(parcel);
      try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
        final String gitPath;
        final SnapshotId sharedCommit;
        Path snapshot = workspace.root().resolve("snapshot");
        try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
          gitPath = lease.content().getParcelGitPath(repository, parcelPath);
          sharedCommit = GitRepo.get(lease.repository()).core().resolveCommit(revision);
          if (!SharedRepositoryService.get(level.getServer())
              .parcelPaths(repository, sharedCommit.value())
              .contains(gitPath)) {
            throw new ParcelException(
                "Parcel path is not listed by shared repository: " + gitPath);
          }
          GitRepo.get(lease.repository()).exportRevision(sharedCommit.value(), gitPath, snapshot);
        }
        ParcelMeta restored = ParcelStorage.validateSnapshot(snapshot, ProgressReporter.NONE);
        ParcelRepositoryService.validateGeometry(parcel.meta(), restored);
        return ParcelRepositoryService.saveWorkspaceSnapshot(
            parcel,
            internalParcelsDirectory(),
            snapshot,
            "Imported snapshot",
            "Imported from %s@%s:%s".formatted(repository, sharedCommit.value(), gitPath),
            identity,
            SnapshotNode.Source.IMPORTED,
            ProgressReporter.NONE);
      }
    } finally {
      lock.unlock();
    }
  }

  /** Server-thread compatibility entry point for commands using the shared repository's HEAD. */
  @ApiStatus.Internal
  public Parcel importSharedParcel(
      String repository, String parcelPath, ParcelTransform transform)
      throws IOException, ParcelException {
    return importSharedSnapshot(
        repository,
        "HEAD",
        parcelPath,
        transform,
        new GitRepo.CommitIdentity("Git Parcel Server", "server@gitparcel.local"));
  }

  public void syncTo(ServerPlayer player) {
    Services.SERVER_NETWORKING.send(
        player, UpdateParcelsMessage.fullSync(savedData.parcels()));
  }

  private InternalRepository internalRepository(Parcel parcel) {
    return ParcelRepositoryService.repository(parcel, internalParcelsDirectory());
  }

  private Path internalParcelsDirectory() {
    return StorageUtils.worldStorage(level.getServer()).getInternalParcelsDir();
  }

  private static void copySnapshot(
      Path source, Path target, ProgressReporter progress) throws IOException {
    if (Files.exists(target)) {
      throw new IOException("Publish target already exists: " + target);
    }
    Files.createDirectories(target);
    try {
      long[] copied = {0};
      Files.walkFileTree(
          source,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path input, BasicFileAttributes attrs)
                throws IOException {
              if (!attrs.isDirectory() || attrs.isSymbolicLink()) {
                throw new IOException("Snapshot contains a special directory: " + input);
              }
              Path output = resolveOnTarget(source, target, input);
              Files.createDirectories(output);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path input, BasicFileAttributes attrs)
                throws IOException {
              Path output = resolveOnTarget(source, target, input);
              if (attrs.isRegularFile() && !attrs.isSymbolicLink()) {
                Files.createDirectories(output.getParent());
                Files.copy(input, output);
                progress.report("publish_files", ++copied[0], "files");
              } else {
                throw new IOException("Snapshot contains a special file: " + input);
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException | RuntimeException e) {
      try {
        ParcelStorage.deleteRecursivelyIfExists(target);
      } catch (IOException cleanup) {
        e.addSuppressed(cleanup);
      }
      throw e;
    }
  }

  private static Path resolveOnTarget(Path source, Path target, Path input) {
    Path output = target;
    for (Path part : source.relativize(input)) {
      output = output.resolve(part.toString());
    }
    return output;
  }

  private static void rollbackPublish(
      SharedRepositoryService.RepositoryLease lease,
      String repository,
      ParcelStorage.RepositoryLocation location,
      SharedContent.RepoMetaSnapshot manifestSnapshot,
      Exception original) {
    try {
      ParcelStorage.deleteRecursivelyIfExists(location.parcelDirectory());
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
    try {
      lease.content().restoreRepoMeta(repository, manifestSnapshot);
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
    try {
      GitRepo.get(location.repository())
          .resetIndexPaths(
              List.of(location.gitPath(), SharedContent.REPOSITORY_MANIFEST_FILE));
    } catch (IOException | GitAPIException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
  }

  private void requireRegistered(Parcel parcel) {
    if (savedData.parcels().get(parcel.uuid()) != parcel) {
      throw new IllegalArgumentException(
          "Parcel %s is not registered in this level".formatted(parcel.uuid()));
    }
  }

  private static ReentrantLock parcelLock(UUID parcelUuid) {
    return PARCEL_LOCKS.computeIfAbsent(parcelUuid, ignored -> new ReentrantLock());
  }

  private static ReentrantLock acquireParcelLock(UUID parcelUuid) throws ParcelException.Busy {
    ReentrantLock lock = parcelLock(parcelUuid);
    if (!lock.tryLock()) {
      throw new ParcelException.Busy("Parcel operation is already in progress: " + parcelUuid);
    }
    return lock;
  }
}
