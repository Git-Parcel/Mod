package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.InternalRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** Server-authoritative snapshot use cases for one parcel-owned internal bare repository. */
public final class ParcelRepositoryService {
  private static final GitRepositoryCore.Identity SERVER_IDENTITY =
      new GitRepositoryCore.Identity("Git Parcel Server", "server@gitparcel.local");

  private ParcelRepositoryService() {}

  public static InternalRepository repository(Parcel parcel, Path internalParcelsDir) {
    return InternalRepository.at(internalParcelsDir, parcel.uuid());
  }

  public static SnapshotId saveSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    return saveSnapshot(
        level,
        parcel,
        internalParcelsDir,
        name,
        description,
        identity,
        ignoreEntities,
        TemporarySnapshotWorkspaceFactory.INSTANCE,
        ProgressReporter.NONE,
        SnapshotNode.Source.SAVED);
  }

  public static SnapshotId saveSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities,
      SnapshotWorkspaceFactory workspaceFactory,
      ProgressReporter progress,
      SnapshotNode.Source source)
      throws IOException, ParcelException {
    try (var workspace = workspaceFactory.create()) {
      Path snapshotRoot = workspace.root().resolve("snapshot");
      var repository = repository(parcel, internalParcelsDir);
      Optional<SnapshotId> baseline =
          repository.prepareSnapshotWorkspace(snapshotRoot, progress);
      captureSnapshot(level, parcel, snapshotRoot, ignoreEntities, progress);
      return saveWorkspaceSnapshot(
          parcel,
          internalParcelsDir,
          snapshotRoot,
          name,
          description,
          identity,
          source,
          progress,
          baseline);
    }
  }

  public static void captureSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path snapshotRoot,
      boolean ignoreEntities,
      ProgressReporter progress)
      throws IOException, ParcelException {
    progress.report("world_capture", 0, "blocks");
    ParcelStorage.captureSnapshot(level, parcel, snapshotRoot, ignoreEntities, progress);
    progress.report("workspace_ready", 1, 1, "snapshots");
  }

  public static SnapshotId saveWorkspaceSnapshot(
      Parcel parcel,
      Path internalParcelsDir,
      Path snapshotRoot,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      SnapshotNode.Source source,
      ProgressReporter progress)
      throws IOException {
    InternalRepository repository = repository(parcel, internalParcelsDir);
    return saveWorkspaceSnapshot(
        parcel,
        internalParcelsDir,
        snapshotRoot,
        name,
        description,
        identity,
        source,
        progress,
        repository.current());
  }

  public static SnapshotId saveWorkspaceSnapshot(
      Parcel parcel,
      Path internalParcelsDir,
      Path snapshotRoot,
      String name,
      String description,
      GitRepo.CommitIdentity identity,
      SnapshotNode.Source source,
      ProgressReporter progress,
      Optional<SnapshotId> expectedParent)
      throws IOException {
    var metadata =
        new InternalRepository.SaveMetadata(
            name,
            description,
            new GitRepositoryCore.Identity(identity.name(), identity.email()),
            SERVER_IDENTITY,
            source,
            Instant.now());
    return repository(parcel, internalParcelsDir)
        .saveSnapshot(snapshotRoot, expectedParent, metadata, progress);
  }

  public static Optional<SnapshotId> prepareSnapshotWorkspace(
      Parcel parcel,
      Path internalParcelsDir,
      Path snapshotRoot,
      ProgressReporter progress)
      throws IOException {
    return repository(parcel, internalParcelsDir)
        .prepareSnapshotWorkspace(snapshotRoot, progress);
  }

  public static SnapshotTreePage querySnapshotTree(
      Parcel parcel,
      Path internalParcelsDir,
      int limit,
      Optional<SnapshotId> cursor)
      throws IOException {
    return repository(parcel, internalParcelsDir)
        .queryTree(parcel.uuid(), limit, cursor);
  }

  public static InternalRepository.RestoreResult restoreSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      SnapshotId snapshotId,
      boolean ignoreEntities,
      SnapshotWorkspaceFactory workspaceFactory,
      ProgressReporter progress)
      throws IOException {
    return restoreSnapshot(
        level,
        parcel,
        internalParcelsDir,
        snapshotId,
        ignoreEntities,
        workspaceFactory,
        progress,
        ServerThreadBridge.DIRECT);
  }

  public static InternalRepository.RestoreResult restoreSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      SnapshotId snapshotId,
      boolean ignoreEntities,
      SnapshotWorkspaceFactory workspaceFactory,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws IOException {
    return repository(parcel, internalParcelsDir)
        .restoreSnapshot(
            snapshotId,
            workspaceFactory,
            snapshotRestorer(level, parcel, ignoreEntities, progress, serverThread),
            progress);
  }

  /** Retries an interrupted restore, or applies its protected pre-restore snapshot. */
  public static InternalRepository.RestoreResult resolvePendingRestore(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      UUID operationId,
      boolean rollback,
      boolean ignoreEntities,
      SnapshotWorkspaceFactory workspaceFactory,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws IOException {
    return repository(parcel, internalParcelsDir)
        .resolvePendingRestore(
            operationId,
            rollback,
            workspaceFactory,
            snapshotRestorer(level, parcel, ignoreEntities, progress, serverThread),
            progress);
  }

  private static InternalRepository.SnapshotRestorer snapshotRestorer(
      ServerLevel level,
      Parcel parcel,
      boolean ignoreEntities,
      ProgressReporter progress,
      ServerThreadBridge serverThread) {
    return new InternalRepository.SnapshotRestorer() {
      @Override
      public void validate(Path snapshotRoot) throws Exception {
        ParcelMeta restored = ParcelStorage.validateSnapshot(snapshotRoot, progress);
        validateGeometry(parcel.meta(), restored);
      }

      @Override
      public void apply(Path snapshotRoot) throws Exception {
        final int loadFlags =
            Block.UPDATE_CLIENTS
                | Block.UPDATE_IMMEDIATE
                | Block.UPDATE_KNOWN_SHAPE
                | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
        serverThread.run(
            () ->
                ParcelStorage.applyValidatedSnapshot(
                    level,
                    parcel.transform(),
                    snapshotRoot,
                    false,
                    ignoreEntities || parcel.meta().getExcludeEntities(),
                    loadFlags,
                    ProgressReporter.prefixed("restore_", progress)));
      }
    };
  }

  public static void validateGeometry(ParcelMeta current, ParcelMeta restored)
      throws ParcelException {
    if (!current.size().equals(restored.size())
        || !current.anchor().equals(restored.anchor())) {
      throw new ParcelException(
          "Cannot restore a snapshot whose size or anchor differs from the registered parcel");
    }
  }
}
