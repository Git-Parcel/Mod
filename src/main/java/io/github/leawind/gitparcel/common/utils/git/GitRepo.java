package io.github.leawind.gitparcel.common.utils.git;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jspecify.annotations.Nullable;

/**
 * Serialized access to one on-disk Git repository.
 *
 * <p>Instances are cached by normalized absolute path so operations targeting the same repository
 * cannot concurrently mutate its index.
 */
public final class GitRepo {

  private static final ConcurrentHashMap<Path, GitRepo> CACHE = new ConcurrentHashMap<>();

  public static GitRepo get(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    return CACHE.computeIfAbsent(normalized, GitRepo::new);
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
    return Files.exists(path.resolve(".git"));
  }

  /**
   * Stages and commits changes below one repository-relative directory.
   *
   * @return the new commit, or an empty result when that directory has no changes
   */
  public synchronized Optional<CommitInfo> commit(
      String repositoryRelativePath, String message, CommitIdentity identity)
      throws IOException, GitAPIException {
    String gitPath = validateGitPath(repositoryRelativePath);
    Objects.requireNonNull(identity, "identity");
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Commit message must not be blank");
    }

    try (Git git = openOrInit()) {
      git.add().addFilepattern(gitPath).call();
      git.add().setUpdate(true).addFilepattern(gitPath).call();

      if (git.status().addPath(gitPath).call().isClean()) {
        return Optional.empty();
      }

      var person = new PersonIdent(identity.name(), identity.email());
      RevCommit commit =
          git.commit()
              .setOnly(gitPath)
              .setMessage(message)
              .setAuthor(person)
              .setCommitter(person)
              .call();
      return Optional.of(toInfo(commit));
    }
  }

  /** Lists newest-first commits which changed the specified parcel directory. */
  public synchronized List<CommitInfo> history(String repositoryRelativePath, int limit)
      throws IOException, GitAPIException {
    String gitPath = validateGitPath(repositoryRelativePath);
    if (limit < 1) {
      throw new IllegalArgumentException("History limit must be positive");
    }

    try (Git git = open()) {
      if (git == null) {
        return List.of();
      }

      var result = new ArrayList<CommitInfo>();
      try {
        for (RevCommit commit :
            git.log().addPath(gitPath).setMaxCount(limit).call()) {
          result.add(toInfo(commit));
        }
      } catch (NoHeadException ignored) {
        return List.of();
      }
      return List.copyOf(result);
    }
  }

  /**
   * Exports the parcel directory from a commit without changing HEAD, the index, or the working
   * tree.
   *
   * @param destination a path which must not already exist
   */
  public synchronized void exportRevision(
      String revision, String repositoryRelativePath, Path destination)
      throws IOException {
    String gitPath = validateGitPath(repositoryRelativePath);
    if (revision == null || revision.isBlank()) {
      throw new IllegalArgumentException("Revision must not be blank");
    }
    Path normalizedDestination = destination.toAbsolutePath().normalize();
    if (Files.exists(normalizedDestination)) {
      throw new IOException("Export destination already exists: " + normalizedDestination);
    }

    try (Git git = open()) {
      if (git == null) {
        throw new IOException("Git repository does not exist: " + path);
      }

      var repository = git.getRepository();
      var commitId = repository.resolve(revision + "^{commit}");
      if (commitId == null) {
        throw new IOException("Unknown Git revision: " + revision);
      }

      boolean found = false;
      Files.createDirectories(normalizedDestination);
      try (var revWalk = new RevWalk(repository);
          var treeWalk = new TreeWalk(repository)) {
        var commit = revWalk.parseCommit(commitId);
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);

        String prefix = gitPath + "/";
        while (treeWalk.next()) {
          String treePath = treeWalk.getPathString();
          if (!treePath.startsWith(prefix)) {
            continue;
          }

          String relativeString = treePath.substring(prefix.length());
          if (relativeString.isEmpty()) {
            continue;
          }

          FileMode mode = treeWalk.getFileMode(0);
          if (!FileMode.REGULAR_FILE.equals(mode)
              && !FileMode.EXECUTABLE_FILE.equals(mode)) {
            throw new IOException(
                "Unsupported Git tree entry type for parcel file: " + treePath);
          }

          Path output = normalizedDestination.resolve(relativeString).normalize();
          if (!output.startsWith(normalizedDestination)) {
            throw new IOException("Git tree entry escapes export directory: " + treePath);
          }

          Files.createDirectories(output.getParent());
          try (OutputStream stream = Files.newOutputStream(output)) {
            repository.open(treeWalk.getObjectId(0)).copyTo(stream);
          }
          found = true;
        }
      } catch (IOException | RuntimeException e) {
        deleteRecursivelyIfExists(normalizedDestination);
        throw e;
      }

      if (!found) {
        deleteRecursivelyIfExists(normalizedDestination);
        throw new IOException(
            "Revision %s does not contain parcel path %s".formatted(revision, gitPath));
      }
    }
  }

  private Git openOrInit() throws IOException, GitAPIException {
    Git existing = open();
    if (existing != null) {
      return existing;
    }

    Files.createDirectories(path);
    return Git.init().setDirectory(file).call();
  }

  /**
   * Opens an existing repository.
   *
   * @return a live Git handle, or {@code null} when the repository has not been initialized
   */
  private @Nullable Git open() throws IOException {
    if (!hasDotGit()) {
      return null;
    }
    return Git.open(file);
  }

  private static String validateGitPath(String path) {
    if (path == null
        || path.isBlank()
        || path.startsWith("/")
        || path.endsWith("/")
        || path.contains("\\")
        || path.contains("//")) {
      throw new IllegalArgumentException("Invalid repository-relative Git path: " + path);
    }
    for (String part : path.split("/")) {
      if (part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Git path must stay inside the repository: " + path);
      }
    }
    return path;
  }

  private static CommitInfo toInfo(RevCommit commit) {
    return new CommitInfo(
        commit.getId().name(),
        Instant.ofEpochSecond(commit.getCommitTime()),
        commit.getAuthorIdent().getName(),
        commit.getShortMessage());
  }

  private static void deleteRecursivelyIfExists(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    }
  }

  public record CommitIdentity(String name, String email) {
    public CommitIdentity {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Commit author name must not be blank");
      }
      if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("Commit author email must not be blank");
      }
    }
  }

  public record CommitInfo(
      String revision, Instant committedAt, String author, String message) {}
}
