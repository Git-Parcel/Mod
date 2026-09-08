package io.github.leawind.gitparcel.server.minecraft.logic.world;

import com.google.common.collect.MapMaker;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelRepositoryService;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.InternalRepository;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Snapshot use cases for one level: save, restore, tree queries, audits, and pending-restore
 * recovery. Writes serialize on the registry's per-parcel lock and the repository path lock.
 */
public final class SnapshotService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotService.class);
  private static final Map<ServerLevel, SnapshotService> INSTANCES =
      new MapMaker().weakKeys().makeMap();

  public static SnapshotService get(ServerLevel level) {
    return INSTANCES.computeIfAbsent(level, SnapshotService::new);
  }

  private final ServerLevel level;
  private final ParcelRegistry registry;

  private SnapshotService(ServerLevel level) {
    this.level = level;
    this.registry = ParcelRegistry.get(level);
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
      GitRepositoryCore.Identity identity,
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
      GitRepositoryCore.Identity identity,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      registry.requireRegistered(parcel);
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
          io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode.Source.SAVED);
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
    registry.requireRegistered(parcel);
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
    serverThread.run(() -> registry.requireRegistered(parcel));
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
      GitRepositoryCore.Identity identity)
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
      GitRepositoryCore.Identity identity,
      ProgressReporter progress)
      throws IOException, ParcelException {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      registry.requireRegistered(parcel);
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
    registry.requireRegistered(parcel);
    return internalRepository(parcel).pendingRestores();
  }

  /** Resolves a durable restore record synchronously. Prefer the background entry point in-game. */
  @ApiStatus.Internal
  public InternalRepository.RestoreResult resolvePendingRestore(
      Parcel parcel, UUID operationId, boolean rollback, boolean ignoreEntities)
      throws IOException, ParcelException {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      registry.requireRegistered(parcel);
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
      GitRepositoryCore.Identity identity,
      boolean ignoreEntities,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      Path snapshotRoot = workspace.root().resolve("snapshot");
      Optional<SnapshotId> baseline =
          ParcelRepositoryService.prepareSnapshotWorkspace(
              parcel, internalParcelsDirectory(), snapshotRoot, progress);
      serverThread.run(
          () -> {
            registry.requireRegistered(parcel);
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
          io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode.Source.SAVED,
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
      GitRepositoryCore.Identity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      serverThread.run(() -> registry.requireRegistered(parcel));
      if (mode == RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE) {
        saveBeforeRestore(
            parcel, snapshotId, ignoreEntities, identity, progress, serverThread);
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

  private void saveBeforeRestore(
      Parcel parcel,
      SnapshotId snapshotId,
      boolean ignoreEntities,
      GitRepositoryCore.Identity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
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
          io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode.Source.SAVED,
          progress,
          baseline);
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
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      serverThread.run(() -> registry.requireRegistered(parcel));
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

  InternalRepository internalRepository(Parcel parcel) {
    return ParcelRepositoryService.repository(parcel, internalParcelsDirectory());
  }

  private Path internalParcelsDirectory() {
    return StorageUtils.worldStorage(level.getServer()).getInternalParcelsDir();
  }
}
