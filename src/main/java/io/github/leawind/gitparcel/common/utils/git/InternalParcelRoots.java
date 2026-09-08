package io.github.leawind.gitparcel.common.utils.git;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the directories that host mod-managed internal parcel repositories.
 *
 * <p>Every {@link InternalRepository} registers its parent directory, and the shared-repository
 * facade refuses to open anything inside those roots. Without this guard, calling shared-repo
 * helpers on an internal bare repository could bypass the single-parent snapshot invariants that
 * only {@link InternalRepository} enforces.
 */
public final class InternalParcelRoots {
  private static final Set<Path> ROOTS = ConcurrentHashMap.newKeySet();

  private InternalParcelRoots() {}

  public static void register(Path parcelsRoot) {
    ROOTS.add(parcelsRoot.toAbsolutePath().normalize());
  }

  /** Throws when a path lies inside a registered internal parcel repositories directory. */
  public static void assertShareable(Path repositoryPath) {
    Path normalized = repositoryPath.toAbsolutePath().normalize();
    for (Path root : ROOTS) {
      if (normalized.startsWith(root)) {
        throw new IllegalArgumentException(
            "Path is inside the mod-managed internal parcel repositories and cannot be opened as"
                + " a shared repository: "
                + repositoryPath);
      }
    }
  }
}
