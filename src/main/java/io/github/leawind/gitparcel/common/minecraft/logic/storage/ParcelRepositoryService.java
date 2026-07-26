package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.eclipse.jgit.api.errors.GitAPIException;

/** Git operations for parcel snapshots, independent from command presentation. */
public final class ParcelRepositoryService {
  private ParcelRepositoryService() {}

  public static Optional<GitRepo.CommitInfo> commit(
      Parcel parcel,
      Path internalParcelsDir,
      String message,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    var location = ParcelStorage.resolveRepositoryLocation(parcel, internalParcelsDir);
    return commit(parcel, location, message, identity);
  }

  public static Optional<GitRepo.CommitInfo> commit(
      Parcel parcel,
      ParcelStorage.RepositoryLocation location,
      String message,
      GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    if (!Files.isRegularFile(location.parcelDirectory().resolve("parcel.json"))) {
      throw new ParcelException("Parcel has not been saved; run save before commit");
    }

    try {
      List<String> commitPaths =
          location.sharedRepository() == null
              ? List.of(location.gitPath())
              : List.of(location.gitPath(), "meta.json");
      return GitRepo.get(location.repository())
          .commit(commitPaths, message, identity);
    } catch (GitAPIException e) {
      throw new ParcelException("Git commit failed", e);
    }
  }

  public static List<GitRepo.CommitInfo> history(
      Parcel parcel, Path internalParcelsDir, int limit)
      throws IOException, ParcelException {
    var location = ParcelStorage.resolveRepositoryLocation(parcel, internalParcelsDir);
    return history(location, limit);
  }

  public static List<GitRepo.CommitInfo> history(
      ParcelStorage.RepositoryLocation location, int limit)
      throws IOException, ParcelException {
    try {
      return GitRepo.get(location.repository()).history(location.gitPath(), limit);
    } catch (GitAPIException e) {
      throw new ParcelException("Failed to read Git history", e);
    }
  }

  /**
   * Loads one committed snapshot into the parcel's current world transform without checking out the
   * repository.
   */
  public static void restore(
      ServerLevel level,
      Parcel parcel,
      Path internalParcelsDir,
      String revision,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    var location = ParcelStorage.resolveRepositoryLocation(parcel, internalParcelsDir);
    restore(level, parcel, location, revision, ignoreEntities);
  }

  public static void restore(
      ServerLevel level,
      Parcel parcel,
      ParcelStorage.RepositoryLocation location,
      String revision,
      boolean ignoreEntities)
      throws IOException, ParcelException {
    Path temporaryRoot = Files.createTempDirectory("gitparcel-restore-");
    Path snapshot = temporaryRoot.resolve("parcel");

    try {
      GitRepo.get(location.repository())
          .exportRevision(revision, location.gitPath(), snapshot);
      validateGeometry(parcel.meta(), ParcelMeta.load(snapshot.resolve("parcel.json")));

      final int loadFlags =
          Block.UPDATE_CLIENTS
              | Block.UPDATE_IMMEDIATE
              | Block.UPDATE_KNOWN_SHAPE
              | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
      ParcelStorage.load(
          level,
          parcel.transform(),
          snapshot,
          false,
          ignoreEntities || parcel.meta().getExcludeEntities(),
          loadFlags);
    } finally {
      try {
        ParcelStorage.deleteRecursivelyIfExists(temporaryRoot);
      } catch (IOException cleanupFailure) {
        ParcelStorage.LOGGER.warn(
            "Failed to remove temporary Git restore directory {}",
            temporaryRoot,
            cleanupFailure);
      }
    }
  }

  public static void validateGeometry(ParcelMeta current, ParcelMeta restored)
      throws ParcelException {
    if (!current.size().equals(restored.size())
        || !current.anchor().equals(restored.anchor())) {
      throw new ParcelException(
          "Cannot restore a revision whose size or anchor differs from the registered parcel");
    }
  }
}
