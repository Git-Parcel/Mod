package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.utils.git.SharedRepository;
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

  /** Locates the archive that stores snapshots of the given parcel. */
  public static ParcelArchive archive(Parcel parcel, Path internalParcelsDir) {
    return ParcelArchive.forParcel(parcel, internalParcelsDir);
  }

  public static SnapshotId saveSnapshot(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      String name,
      String description,
      GitRepositoryCore.Identity identity,
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
      GitRepositoryCore.Identity identity,
      boolean ignoreEntities,
      SnapshotWorkspaceFactory workspaceFactory,
      ProgressReporter progress,
      SnapshotNode.Source source)
      throws IOException, ParcelException {
    try (var workspace = workspaceFactory.create()) {
      Path snapshotRoot = workspace.root().resolve("snapshot");
      var repository = archive(parcel, internalParcelsDir).repository();
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
      GitRepositoryCore.Identity identity,
      SnapshotNode.Source source,
      ProgressReporter progress)
      throws IOException {
    InternalRepository repository = archive(parcel, internalParcelsDir).repository();
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
      GitRepositoryCore.Identity identity,
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
    return archive(parcel, internalParcelsDir).repository()
        .saveSnapshot(snapshotRoot, expectedParent, metadata, progress);
  }

  public static Optional<SnapshotId> prepareSnapshotWorkspace(
      Parcel parcel,
      Path internalParcelsDir,
      Path snapshotRoot,
      ProgressReporter progress)
      throws IOException {
    return archive(parcel, internalParcelsDir).repository()
        .prepareSnapshotWorkspace(snapshotRoot, progress);
  }

  public static SnapshotTreePage querySnapshotTree(
      Parcel parcel,
      Path internalParcelsDir,
      int limit,
      Optional<SnapshotId> cursor)
      throws IOException {
    return archive(parcel, internalParcelsDir).repository()
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
    return archive(parcel, internalParcelsDir).repository()
        .restoreSnapshot(
            snapshotId,
            workspaceFactory,
            snapshotRestorer(level, parcel, Optional.of(snapshotId), ignoreEntities, progress, serverThread),
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
    return archive(parcel, internalParcelsDir).repository()
        .resolvePendingRestore(
            operationId,
            rollback,
            workspaceFactory,
            snapshotRestorer(level, parcel, Optional.empty(), ignoreEntities, progress, serverThread),
            progress);
  }

  private static InternalRepository.SnapshotRestorer snapshotRestorer(
      ServerLevel level,
      Parcel parcel,
      Optional<SnapshotId> validatedCommit,
      boolean ignoreEntities,
      ProgressReporter progress,
      ServerThreadBridge serverThread) {
    return new InternalRepository.SnapshotRestorer() {
      @Override
      public void validate(Path snapshotRoot) throws Exception {
        // Restores keep the parcel placement and load the snapshot's own geometry without a
        // boundary check: bounds changes since the last sync are a structural signal, not an error.
        if (validatedCommit.isPresent()) {
          ParcelStorage.validateSnapshotCached(snapshotRoot, validatedCommit.orElseThrow(), progress);
        } else {
          ParcelStorage.validateSnapshot(snapshotRoot, progress);
        }
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
}
