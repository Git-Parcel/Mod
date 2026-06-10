package io.github.leawind.gitparcel.core.util.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;

public final class GitRepo {

  private static final ConcurrentHashMap<Path, GitRepo> CACHE = new ConcurrentHashMap<>();

  public static GitRepo get(Path path) {
    return CACHE.computeIfAbsent(path.normalize(), GitRepo::new);
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
