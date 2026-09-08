package io.github.leawind.gitparcel.server.minecraft.logic.world;

import com.google.common.collect.MapMaker;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelRepositoryService;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.SharedRepository;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedContent;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.ApiStatus;

/**
 * Publishes saved internal snapshots into shared repositories and imports shared commits back,
 * never capturing or writing the live world implicitly.
 */
public final class PublishImportService {
  private static final int LOAD_FLAGS =
      Block.UPDATE_CLIENTS
          | Block.UPDATE_IMMEDIATE
          | Block.UPDATE_KNOWN_SHAPE
          | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;

  private static final Map<ServerLevel, PublishImportService> INSTANCES =
      new MapMaker().weakKeys().makeMap();

  public static PublishImportService get(ServerLevel level) {
    return INSTANCES.computeIfAbsent(level, PublishImportService::new);
  }

  private final ServerLevel level;
  private final ParcelRegistry registry;
  private final SnapshotService snapshots;

  private PublishImportService(ServerLevel level) {
    this.level = level;
    this.registry = ParcelRegistry.get(level);
    this.snapshots = SnapshotService.get(level);
  }

  /** Server-thread entry point. Prefer {@link #publishCurrentSnapshotInBackground} in-game. */
  @ApiStatus.Internal
  public Optional<SharedRepository.CommitInfo> publishSnapshot(
      Parcel parcel,
      SnapshotId snapshotId,
      String repository,
      String parcelPath,
      String message,
      GitRepositoryCore.Identity identity)
      throws IOException, ParcelException {
    registry.requireRegistered(parcel);
    return publishSnapshotUnchecked(
        parcel,
        snapshotId,
        repository,
        parcelPath,
        message,
        identity,
        ProgressReporter.NONE);
  }

  /** Server-thread entry point: publishes the current baseline, if one exists. */
  @ApiStatus.Internal
  public Optional<SharedRepository.CommitInfo> publishCurrentSnapshot(
      Parcel parcel,
      String repository,
      String parcelPath,
      String message,
      GitRepositoryCore.Identity identity)
      throws IOException, ParcelException {
    SnapshotId current =
        snapshots
            .internalRepository(parcel)
            .current()
            .orElseThrow(() -> new ParcelException("Parcel has no saved snapshot to publish"));
    return publishSnapshot(parcel, current, repository, parcelPath, message, identity);
  }

  /** Publishes the current saved baseline with all repository I/O on the calling worker. */
  public SharedRepository.CommitInfo publishCurrentSnapshotInBackground(
      Parcel parcel,
      String repository,
      String parcelPath,
      String message,
      GitRepositoryCore.Identity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    serverThread.run(() -> registry.requireRegistered(parcel));
    SnapshotId current =
        snapshots
            .internalRepository(parcel)
            .current()
            .orElseThrow(() -> new ParcelException("Parcel has no saved snapshot to publish"));
    return publishSnapshotUnchecked(
            parcel, current, repository, parcelPath, message, identity, progress)
        .orElseThrow(() -> new IOException("Publishing produced no shared repository commit"));
  }

  private Optional<SharedRepository.CommitInfo> publishSnapshotUnchecked(
      Parcel parcel,
      SnapshotId snapshotId,
      String repository,
      String parcelPath,
      String message,
      GitRepositoryCore.Identity identity,
      ProgressReporter progress)
      throws IOException, ParcelException {
    ProgressReporter reporter = ProgressReporter.safe(progress);
    snapshots.internalRepository(parcel).current(); // also performs the repository health check

    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      Path exported = workspace.root().resolve("snapshot");
      snapshots.internalRepository(parcel).exportSnapshot(snapshotId, exported, reporter);
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
          SharedRepository.CommitInfo result =
              SharedRepository.get(location.repository())
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

  /** Server-thread entry point: applies imported content to the world on the calling thread. */
  @ApiStatus.Internal
  public Parcel importSharedSnapshot(
      String repository,
      String revision,
      String parcelPath,
      ParcelTransform transform,
      GitRepositoryCore.Identity identity)
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
      GitRepositoryCore.Identity identity,
      ProgressReporter progress,
      ServerThreadBridge serverThread)
      throws Exception {
    try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
      final String gitPath;
      final SnapshotId sharedCommit;
      Path snapshot = workspace.root().resolve("snapshot");
      try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
        gitPath = lease.content().getParcelGitPath(repository, parcelPath);
        sharedCommit = SharedRepository.get(lease.repository()).resolveCommit(revision);
        if (!SharedRepositoryService.get(level.getServer())
            .parcelPaths(repository, sharedCommit.value())
            .contains(gitPath)) {
          throw new ParcelException("Parcel path is not listed by shared repository: " + gitPath);
        }
        SharedRepository.get(lease.repository()).exportRevision(sharedCommit.value(), gitPath, snapshot);
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
            registry.validateNewParcel(parcel);
            ParcelStorage.applyValidatedSnapshot(
                level,
                transform,
                snapshot,
                false,
                meta.getExcludeEntities(),
                LOAD_FLAGS,
                ProgressReporter.prefixed("import_", progress));
            registry.addNewParcel(parcel);
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
      GitRepositoryCore.Identity identity)
      throws IOException, ParcelException {
    var lock = registry.acquireParcelLock(parcel.uuid());
    try {
      registry.requireRegistered(parcel);
      try (var workspace = TemporarySnapshotWorkspaceFactory.INSTANCE.create()) {
        final String gitPath;
        final SnapshotId sharedCommit;
        Path snapshot = workspace.root().resolve("snapshot");
        try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
          gitPath = lease.content().getParcelGitPath(repository, parcelPath);
          sharedCommit = SharedRepository.get(lease.repository()).resolveCommit(revision);
          if (!SharedRepositoryService.get(level.getServer())
              .parcelPaths(repository, sharedCommit.value())
              .contains(gitPath)) {
            throw new ParcelException(
                "Parcel path is not listed by shared repository: " + gitPath);
          }
          SharedRepository.get(lease.repository()).exportRevision(sharedCommit.value(), gitPath, snapshot);
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
        new GitRepositoryCore.Identity("Git Parcel Server", "server@gitparcel.local"));
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
      SharedRepository.get(location.repository())
          .resetIndexPaths(
              List.of(location.gitPath(), SharedContent.REPOSITORY_MANIFEST_FILE));
    } catch (IOException | GitAPIException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
  }
}
