package io.github.leawind.gitparcel.utils.git;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;

public final class GitRepo {

  private static final LoadingCache<Path, GitRepo> CACHE =
      Caffeine.newBuilder()
          .maximumSize(64)
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .weakValues()
          .build(GitRepo::new);

  public static GitRepo get(Path path) {
    return CACHE.get(path.normalize());
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
