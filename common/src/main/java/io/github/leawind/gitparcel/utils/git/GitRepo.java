package io.github.leawind.gitparcel.utils.git;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;

public final class GitRepo {
  private static final Map<Path, WeakReference<GitRepo>> CACHE = new ConcurrentHashMap<>();

  public static GitRepo get(Path path) {
    var normalizedPath = path.normalize();
    return CACHE
        .compute(
            normalizedPath,
            (key, ref) -> {
              if (ref != null && ref.get() != null) {
                return ref;
              }
              return new WeakReference<>(new GitRepo(normalizedPath));
            })
        .get();
  }

  private final Path path;
  private final File file;

  private GitRepo(Path path) {
    this.path = path;
    this.file = path.toFile();
  }

  public Path path() {
    return path;
  }

  public boolean hasDotGit() {
    return Files.isDirectory(path.resolve(".git"));
  }

  public Git openOrInit() throws IOException, GitAPIException {
    if (hasDotGit()) {
      return Git.open(file);
    }

    Files.createDirectories(path);

    try (Git git = Git.init().setDirectory(file).call()) {
      return git;
    }
  }

  /**
   * Opens an existing git repository for this parcel.
   *
   * @return The Git instance for the repository, or null if the repository doesn't exist
   * @throws IOException If an I/O error occurs
   */
  public @Nullable Git open() throws IOException {
    if (!hasDotGit()) {
      return null;
    }
    return Git.open(file);
  }
}
