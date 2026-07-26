package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.world.ParcelValidator;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelRepositoryService;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedContent;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;

/**
 * Coordinates parcel state, storage, and client synchronization for one server level.
 *
 * <p>{@link Parcel} remains a serializable domain object; all operations that require a live
 * Minecraft level are kept here.
 */
public final class ParcelService {
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
    validateNewParcel(parcel);

    savedData.addParcel(parcel);
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  public void validateNewParcel(Parcel parcel) throws IllegalArgumentException {
    var maxParcelVolume = GitParcelWorldSavedData.get(level.getServer()).maxParcelVolume();
    ParcelValidator.validateNewParcel(parcel, savedData.parcels().values(), maxParcelVolume);
  }

  public @Nullable Parcel deleteParcel(UUID uuid) {
    var deleted = savedData.removeParcel(uuid);
    if (deleted != null) {
      Services.SERVER_NETWORKING.sendToAllPlayers(
          level, UpdateParcelsMessage.removals(Set.of(uuid)));
    }
    return deleted;
  }

  /** Marks a mutated parcel as dirty and synchronizes it to clients. */
  public void updateParcel(Parcel parcel) {
    if (savedData.parcels().get(parcel.uuid()) != parcel) {
      throw new IllegalArgumentException(
          "Parcel %s is not registered in this level".formatted(parcel.uuid()));
    }

    savedData.setDirty();
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  public Path getParcelDirectory(Parcel parcel) throws IOException {
    return resolveRepositoryLocation(parcel).parcelDirectory();
  }

  public void saveParcel(Parcel parcel, boolean ignoreEntities)
      throws IOException, ParcelException {
    var shared = parcel.location().flatMap(Parcel.ParcelLocation::sharedRepository);
    if (shared.isPresent()) {
      try (var lease =
          SharedRepositoryService.get(level.getServer()).acquire(shared.orElseThrow())) {
        ParcelStorage.save(
            level,
            parcel,
            sharedLocation(parcel, lease).parcelDirectory(),
            ignoreEntities);
      }
      return;
    }
    ParcelStorage.save(level, parcel, getParcelDirectory(parcel), ignoreEntities);
  }

  public Optional<GitRepo.CommitInfo> commitParcel(
      Parcel parcel, String message, GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    var shared = parcel.location().flatMap(Parcel.ParcelLocation::sharedRepository);
    if (shared.isPresent()) {
      try (var lease =
          SharedRepositoryService.get(level.getServer()).acquire(shared.orElseThrow())) {
        return ParcelRepositoryService.commit(
            parcel, sharedLocation(parcel, lease), message, identity);
      }
    }
    return ParcelRepositoryService.commit(
        parcel, resolveRepositoryLocation(parcel), message, identity);
  }

  public List<GitRepo.CommitInfo> getParcelHistory(Parcel parcel, int limit)
      throws IOException, ParcelException {
    var shared = parcel.location().flatMap(Parcel.ParcelLocation::sharedRepository);
    if (shared.isPresent()) {
      try (var lease =
          SharedRepositoryService.get(level.getServer()).acquire(shared.orElseThrow())) {
        return ParcelRepositoryService.history(sharedLocation(parcel, lease), limit);
      }
    }
    return ParcelRepositoryService.history(resolveRepositoryLocation(parcel), limit);
  }

  public void restoreParcel(Parcel parcel, String revision, boolean ignoreEntities)
      throws IOException, ParcelException {
    var shared = parcel.location().flatMap(Parcel.ParcelLocation::sharedRepository);
    if (shared.isPresent()) {
      try (var lease =
          SharedRepositoryService.get(level.getServer()).acquire(shared.orElseThrow())) {
        ParcelRepositoryService.restore(
            level,
            parcel,
            sharedLocation(parcel, lease),
            revision,
            ignoreEntities);
      }
      return;
    }
    ParcelRepositoryService.restore(
        level, parcel, resolveRepositoryLocation(parcel), revision, ignoreEntities);
  }

  /** Binds an existing registered parcel to an existing shared snapshot without loading it. */
  public void bindParcel(Parcel parcel, String repository, String parcelPath)
      throws IOException, ParcelException {
    requireRegistered(parcel);
    try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
      var location = requireSharedParcel(lease, parcelPath);
      ParcelMeta stored = ParcelMeta.load(location.parcelDirectory().resolve("parcel.json"));
      ParcelRepositoryService.validateGeometry(parcel.meta(), stored);
      parcel.setLocation(
          Parcel.ParcelLocation.shared(repository, location.relative()));
      updateParcel(parcel);
    }
  }

  /** Stops using external/shared storage; future saves return to the world-internal repository. */
  public void unbindParcel(Parcel parcel) {
    requireRegistered(parcel);
    parcel.setLocation(null);
    updateParcel(parcel);
  }

  /** Publishes an internally stored parcel into a new path in a shared repository and commits it. */
  public Optional<GitRepo.CommitInfo> publishParcel(
      Parcel parcel,
      String repository,
      String parcelPath,
      String message,
      GitRepo.CommitIdentity identity,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    requireRegistered(parcel);
    if (parcel.location().isPresent()) {
      throw new ParcelException("Only world-internal parcels can be published");
    }

    try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
      String gitPath = lease.content().getParcelGitPath(repository, parcelPath);
      Path relative = lease.repository().getFileSystem().getPath(gitPath);
      var location =
          new ParcelStorage.RepositoryLocation(
              lease.repository(), relative, repository);
      if (lease.content().containsParcelPath(repository, gitPath)
          || Files.exists(location.parcelDirectory())) {
        throw new ParcelException("Shared parcel path already exists: " + gitPath);
      }

      var metaSnapshot = lease.content().snapshotRepoMeta(repository);
      GitRepo.CommitInfo commit;
      try {
        ParcelStorage.save(level, parcel, location.parcelDirectory(), ignoreEntities);
        lease.content().addParcelPath(repository, gitPath);
        commit =
            ParcelRepositoryService.commit(parcel, location, message, identity)
                .orElseThrow(
                    () ->
                        new ParcelException(
                            "Published snapshot produced no Git changes"));
      } catch (IOException | ParcelException | RuntimeException e) {
        rollbackPublish(lease, repository, location, metaSnapshot, e);
        throw e;
      }
      parcel.setLocation(Parcel.ParcelLocation.shared(repository, relative));
      updateParcel(parcel);
      return Optional.of(commit);
    }
  }

  private static void rollbackPublish(
      SharedRepositoryService.RepositoryLease lease,
      String repository,
      ParcelStorage.RepositoryLocation location,
      SharedContent.RepoMetaSnapshot metaSnapshot,
      Exception original) {
    try {
      ParcelStorage.deleteRecursivelyIfExists(location.parcelDirectory());
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
    try {
      lease.content().restoreRepoMeta(repository, metaSnapshot);
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
    try {
      GitRepo.get(location.repository())
          .resetIndexPaths(List.of(location.gitPath(), "meta.json"));
    } catch (IOException | GitAPIException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
  }

  /** Loads and registers a parcel from a shared repository's current working tree. */
  public Parcel importSharedParcel(
      String repository, String parcelPath, ParcelTransform transform)
      throws IOException, ParcelException {
    try (var lease = SharedRepositoryService.get(level.getServer()).acquire(repository)) {
      var location = requireSharedParcel(lease, parcelPath);
      ParcelMeta meta = ParcelMeta.load(location.parcelDirectory().resolve("parcel.json"));
      var permissions =
          GitParcelWorldSavedData.get(level.getServer()).parcelDefaultPermissions().copy();
      var parcel =
          Parcel.create(
              meta,
              transform,
              permissions,
              Parcel.ParcelLocation.shared(repository, location.relative()));
      validateNewParcel(parcel);
      ParcelStorage.load(
          level,
          transform,
          location.parcelDirectory(),
          false,
          meta.getExcludeEntities(),
          LOAD_FLAGS);
      addNewParcel(parcel);
      return parcel;
    }
  }

  public void syncTo(ServerPlayer player) {
    Services.SERVER_NETWORKING.send(
        player, UpdateParcelsMessage.fullSync(savedData.parcels()));
  }

  private Path getInternalParcelsDirectory() {
    return StorageUtils.worldStorage(level.getServer()).getInternalParcelsDir();
  }

  private ParcelStorage.RepositoryLocation resolveRepositoryLocation(Parcel parcel)
      throws IOException {
    var shared = parcel.location().flatMap(Parcel.ParcelLocation::sharedRepository);
    if (shared.isPresent()) {
      var service = SharedRepositoryService.get(level.getServer());
      Path repository = service.repositoryPath(shared.orElseThrow());
      Path relative = parcel.location().orElseThrow().relative();
      return new ParcelStorage.RepositoryLocation(
          repository, relative, shared.orElseThrow());
    }
    return ParcelStorage.resolveRepositoryLocation(parcel, getInternalParcelsDirectory());
  }

  private ParcelStorage.RepositoryLocation sharedLocation(
      Parcel parcel, SharedRepositoryService.RepositoryLease lease)
      throws IOException, ParcelException {
    String gitPath =
        lease
            .content()
            .getParcelGitPath(
                lease.name(), parcel.location().orElseThrow().relative().toString());
    if (!lease.content().containsParcelPath(lease.name(), gitPath)) {
      throw new ParcelException(
          "Parcel path is not listed by shared repository: " + gitPath);
    }
    Path relative = lease.repository().getFileSystem().getPath(gitPath);
    return new ParcelStorage.RepositoryLocation(
        lease.repository(), relative, lease.name());
  }

  private ParcelStorage.RepositoryLocation requireSharedParcel(
      SharedRepositoryService.RepositoryLease lease, String parcelPath)
      throws IOException, ParcelException {
    String gitPath = lease.content().getParcelGitPath(lease.name(), parcelPath);
    if (!lease.content().containsParcelPath(lease.name(), gitPath)) {
      throw new ParcelException(
          "Parcel path is not listed by shared repository: " + gitPath);
    }
    Path relative = lease.repository().getFileSystem().getPath(gitPath);
    var location =
        new ParcelStorage.RepositoryLocation(
            lease.repository(), relative, lease.name());
    if (!Files.isRegularFile(location.parcelDirectory().resolve("parcel.json"))) {
      throw new ParcelException("Shared parcel is missing parcel.json: " + gitPath);
    }
    return location;
  }

  private void requireRegistered(Parcel parcel) {
    if (savedData.parcels().get(parcel.uuid()) != parcel) {
      throw new IllegalArgumentException(
          "Parcel %s is not registered in this level".formatted(parcel.uuid()));
    }
  }
}
