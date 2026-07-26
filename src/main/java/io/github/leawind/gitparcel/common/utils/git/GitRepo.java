package io.github.leawind.gitparcel.common.utils.git;

import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
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
  private final GitRepositoryCore core;

  private GitRepo(Path path) {
    this.path = path;
    this.file = path.toFile();
    this.core = new GitRepositoryCore(path, RepositoryPolicy.SHARED);
  }

  public Path path() {
    return path;
  }

  public GitRepositoryCore core() {
    return core;
  }

  public boolean hasDotGit() {
    return Files.exists(path.resolve(".git"));
  }

  /** Initializes an empty working-tree repository if necessary. */
  public synchronized void initialize() throws IOException, GitAPIException {
    try (Git ignored = openOrInit()) {
      // Initialization is performed by openOrInit.
    }
  }

  /** Clones into a destination which must not already exist. */
  public static GitRepo cloneRepository(
      String remoteUri,
      Path destination,
      @Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    return GitRepo.get(destination).cloneFrom(remoteUri, credentialsProvider);
  }

  private synchronized GitRepo cloneFrom(
      String remoteUri, @Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    if (remoteUri == null || remoteUri.isBlank()) {
      throw new IllegalArgumentException("Remote URI must not be blank");
    }
    if (Files.exists(path)) {
      throw new IOException("Clone destination already exists: " + path);
    }

    try {
      var command = Git.cloneRepository().setURI(remoteUri).setDirectory(file);
      if (credentialsProvider != null) {
        command.setCredentialsProvider(credentialsProvider);
      }
      try (Git ignored = command.call()) {
        return this;
      }
    } catch (GitAPIException | RuntimeException e) {
      try {
        deleteRecursivelyIfExists(path);
      } catch (IOException cleanupFailure) {
        e.addSuppressed(cleanupFailure);
      }
      throw e;
    }
  }

  /** Fetches tracking references from {@code origin}. */
  public synchronized int fetch(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    try (Git git = requireOpen()) {
      var command = git.fetch().setRemote("origin");
      if (credentialsProvider != null) {
        command.setCredentialsProvider(credentialsProvider);
      }
      return command.call().getTrackingRefUpdates().size();
    }
  }

  /** Pulls from {@code origin}, accepting only a fast-forward update. */
  public synchronized String pull(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    try (Git git = requireOpen()) {
      if (!git.status().call().isClean()) {
        throw new IOException("Cannot pull with uncommitted repository changes: " + path);
      }

      var command = git.pull().setRemote("origin").setFastForward(FastForwardMode.FF_ONLY);
      if (credentialsProvider != null) {
        command.setCredentialsProvider(credentialsProvider);
      }
      var result = command.call();
      if (!result.isSuccessful()) {
        throw new IOException("Git pull was not successful: " + result);
      }
      if (result.getMergeResult() != null) {
        return result.getMergeResult().getMergeStatus().toString();
      }
      if (result.getRebaseResult() != null) {
        return result.getRebaseResult().getStatus().toString();
      }
      return "FETCHED";
    }
  }

  /** Pushes the current branch to its configured upstream. */
  public synchronized int push(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    try (Git git = requireOpen()) {
      if (!git.status().call().isClean()) {
        throw new IOException("Cannot push with uncommitted repository changes: " + path);
      }

      var command = git.push().setRemote("origin");
      String fullBranch = git.getRepository().getFullBranch();
      if (fullBranch == null || !fullBranch.startsWith("refs/heads/")) {
        throw new IOException("Cannot push from a detached or unborn branch: " + path);
      }
      String branch = Repository.shortenRefName(fullBranch);
      command.setRefSpecs(new RefSpec("HEAD:refs/heads/" + branch));
      if (credentialsProvider != null) {
        command.setCredentialsProvider(credentialsProvider);
      }

      int updates = 0;
      for (var result : command.call()) {
        for (RemoteRefUpdate update : result.getRemoteUpdates()) {
          var status = update.getStatus();
          if (status != RemoteRefUpdate.Status.OK
              && status != RemoteRefUpdate.Status.UP_TO_DATE) {
            throw new IOException(
                "Git push rejected %s: %s".formatted(update.getRemoteName(), status));
          }
          if (status == RemoteRefUpdate.Status.OK) {
            updates++;
          }
        }
      }
      return updates;
    }
  }

  /**
   * Stages and commits changes below one repository-relative directory.
   *
   * @return the new commit, or an empty result when that directory has no changes
   */
  public synchronized Optional<CommitInfo> commit(
      String repositoryRelativePath, String message, CommitIdentity identity)
      throws IOException, GitAPIException {
    return commit(List.of(repositoryRelativePath), message, identity);
  }

  /** Restores selected index entries to {@code HEAD} without changing the working tree. */
  public synchronized void resetIndexPaths(Collection<String> repositoryRelativePaths)
      throws IOException, GitAPIException {
    if (repositoryRelativePaths == null || repositoryRelativePaths.isEmpty()) {
      throw new IllegalArgumentException("At least one Git path is required");
    }
    var gitPaths = new LinkedHashSet<String>();
    repositoryRelativePaths.forEach(path -> gitPaths.add(validateGitPath(path)));

    try (Git git = requireOpen()) {
      if (git.getRepository().resolve(Constants.HEAD) != null) {
        var reset = git.reset();
        gitPaths.forEach(reset::addPath);
        reset.call();
      } else {
        var cache = git.getRepository().lockDirCache();
        try {
          var indexedPaths = new ArrayList<String>();
          for (int i = 0; i < cache.getEntryCount(); i++) {
            String indexedPath = cache.getEntry(i).getPathString();
            if (gitPaths.stream()
                .anyMatch(path -> indexedPath.equals(path) || indexedPath.startsWith(path + "/"))) {
              indexedPaths.add(indexedPath);
            }
          }
          var editor = cache.editor();
          indexedPaths.forEach(path -> editor.add(new DirCacheEditor.DeletePath(path)));
          editor.commit();
        } finally {
          cache.unlock();
        }
      }
    }
  }

  /** Stages and commits changes below one or more repository-relative paths. */
  public synchronized Optional<CommitInfo> commit(
      Collection<String> repositoryRelativePaths,
      String message,
      CommitIdentity identity)
      throws IOException, GitAPIException {
    if (repositoryRelativePaths == null || repositoryRelativePaths.isEmpty()) {
      throw new IllegalArgumentException("At least one Git path is required");
    }
    var gitPaths = new LinkedHashSet<String>();
    repositoryRelativePaths.forEach(path -> gitPaths.add(validateGitPath(path)));
    Objects.requireNonNull(identity, "identity");
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Commit message must not be blank");
    }

    try (Git git = openOrInit()) {
      for (String gitPath : gitPaths) {
        git.add().addFilepattern(gitPath).call();
        git.add().setUpdate(true).addFilepattern(gitPath).call();
      }

      boolean clean = true;
      for (String gitPath : gitPaths) {
        clean &= git.status().addPath(gitPath).call().isClean();
      }
      if (clean) {
        return Optional.empty();
      }

      var person = new PersonIdent(identity.name(), identity.email());
      var command =
          git.commit()
              .setMessage(message)
              .setAuthor(person)
              .setCommitter(person);
      gitPaths.forEach(command::setOnly);
      RevCommit commit = command.call();
      return Optional.of(toInfo(commit));
    }
  }

  /** Lists newest-first commits which changed the specified parcel directory. */
  public synchronized List<CommitInfo> history(String repositoryRelativePath, int limit)
      throws IOException, GitAPIException {
    return historyPage(repositoryRelativePath, limit, null).commits();
  }

  /** Lists one cursor-based page of newest-first commits for a repository-relative path. */
  public synchronized HistoryPage historyPage(
      String repositoryRelativePath, int limit, @Nullable String beforeRevision)
      throws IOException, GitAPIException {
    String gitPath = validateGitPath(repositoryRelativePath);
    if (limit < 1) {
      throw new IllegalArgumentException("History limit must be positive");
    }
    if (beforeRevision != null && beforeRevision.isBlank()) {
      throw new IllegalArgumentException("History cursor must not be blank");
    }

    try (Git git = open()) {
      if (git == null) {
        return new HistoryPage(List.of(), Optional.empty());
      }

      var result = new ArrayList<CommitInfo>();
      try {
        var command = git.log().addPath(gitPath);
        org.eclipse.jgit.lib.ObjectId cursorId = null;
        if (beforeRevision != null) {
          cursorId = git.getRepository().resolve(beforeRevision + "^{commit}");
          if (cursorId == null) {
            throw new IOException("Unknown Git history cursor: " + beforeRevision);
          }
          command.add(cursorId);
        }

        boolean cursorSeen = beforeRevision == null;
        for (RevCommit commit : command.call()) {
          if (!cursorSeen) {
            if (!commit.getId().equals(cursorId)) {
              throw new IOException(
                  "Git history cursor does not belong to parcel path: " + beforeRevision);
            }
            cursorSeen = true;
            continue;
          }
          result.add(toInfo(commit));
          if (result.size() > limit) {
            break;
          }
        }
        if (!cursorSeen) {
          throw new IOException(
              "Git history cursor does not belong to parcel path: " + beforeRevision);
        }
      } catch (NoHeadException ignored) {
        return new HistoryPage(List.of(), Optional.empty());
      }

      boolean hasMore = result.size() > limit;
      if (hasMore) {
        result.removeLast();
      }
      Optional<String> nextCursor =
          hasMore ? Optional.of(result.getLast().revision()) : Optional.empty();
      return new HistoryPage(List.copyOf(result), nextCursor);
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
    core.exportResolvedSubtree(
        revision,
        gitPath,
        destination,
        SnapshotTreeLimits.DEFAULT,
        ProgressReporter.NONE);
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

  private Git requireOpen() throws IOException {
    Git git = open();
    if (git == null) {
      throw new IOException("Git repository does not exist: " + path);
    }
    return git;
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
    NioFileTree.deleteRecursivelyIfExists(directory);
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

  public record HistoryPage(List<CommitInfo> commits, Optional<String> nextCursor) {
    public HistoryPage {
      commits = List.copyOf(commits);
      nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
  }
}
