package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.utils.git.InternalRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The storage-side archive of one parcel.
 *
 * <p>An archive is the portable, world-independent dataset of a parcel: its snapshot tree and
 * current baseline live in a dedicated internal bare Git repository. Archives never record world
 * placement; a parcel references its archive implicitly by identity (the parcel UUID) instead of a
 * filesystem path, and this type is that reference. The repository directory is derived as {@code
 * <internalParcelsDir>/<archive-id>.git}, so an archive can only be reached through the parcel that
 * owns it, and deleting a parcel registration never deletes its archive.
 *
 * <p>Cache archive metadata on the parcel with {@link Parcel#setArchiveSync} after each sync so
 * rendering and structural checks do not have to open the repository.
 */
public final class ParcelArchive {
  private final UUID id;
  private final Path directory;

  private ParcelArchive(UUID id, Path directory) {
    this.id = id;
    this.directory = directory;
  }

  /** Locates the archive of one parcel beneath the world's internal parcels directory. */
  public static ParcelArchive forParcel(Parcel parcel, Path internalParcelsDir) {
    return at(internalParcelsDir, parcel.uuid());
  }

  /** Locates an archive by its identity beneath the world's internal parcels directory. */
  public static ParcelArchive at(Path internalParcelsDir, UUID archiveId) {
    return new ParcelArchive(
        archiveId, internalParcelsDir.resolve(archiveId + ".git").toAbsolutePath().normalize());
  }

  /** The archive identity; equal to the UUID of the parcel that owns the archive. */
  public UUID id() {
    return id;
  }

  /** The internal bare repository directory carrying this archive. */
  public Path directory() {
    return directory;
  }

  public boolean exists() {
    return Files.isDirectory(directory);
  }

  /** The internal bare repository that stores the archive's snapshot tree. */
  public InternalRepository repository() {
    return InternalRepository.at(directory.getParent(), id);
  }

  /** Reads the portable metadata recorded by one snapshot of this archive. */
  public ParcelMeta readSnapshotMeta(SnapshotId snapshot) throws IOException {
    return repository().readSnapshotMeta(snapshot);
  }

  /**
   * Collects the archive metadata cached on the parcel after a sync: the synced snapshot's
   * geometry plus the repository's current on-disk size.
   */
  public Parcel.ArchiveSync readSyncState(SnapshotId snapshot) throws IOException {
    ParcelMeta meta = readSnapshotMeta(snapshot);
    return new Parcel.ArchiveSync(meta.size(), meta.anchor(), sizeBytes());
  }

  /** Sums the size of all regular files in the repository directory; zero when it is absent. */
  public long sizeBytes() throws IOException {
    if (!exists()) {
      return 0;
    }
    try (Stream<Path> files = Files.walk(directory)) {
      return files.filter(Files::isRegularFile).mapToLong(ParcelArchive::fileSize).sum();
    }
  }

  private static long fileSize(Path file) {
    try {
      return Files.size(file);
    } catch (IOException e) {
      return 0;
    }
  }
}
