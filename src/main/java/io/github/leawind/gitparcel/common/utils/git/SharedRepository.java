package io.github.leawind.gitparcel.common.utils.git;

import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
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
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.jspecify.annotations.Nullable;

/**
 * Working-tree Git repository for shared content exchange, symmetric to {@link
 * InternalRepository}: both reuse {@link GitRepositoryCore} primitives and the same per-path lock,
 * while this facade additionally allows worktree and remote operations that the internal policy
 * denies. Capabilities are enforced at every method entry, not by hiding commands.
 */
public final class SharedRepository {

  private static final ConcurrentHashMap<Path, SharedRepository> CACHE = new ConcurrentHashMap<>();

  public static SharedRepository get(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    InternalParcelRoots.assertShareable(normalized);
    return CACHE.computeIfAbsent(normalized, SharedRepository::new);
  }

  /** Clones into a destination which must not already exist. */
  public static SharedRepository cloneRepository(
      String remoteUri,
      Path destination,
      @Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    InternalParcelRoots.assertShareable(destination);
    return SharedRepository.get(destination).cloneFrom(remoteUri, credentialsProvider);
  }

  private final Path path;
  private final File file;
  private final GitRepositoryCore core;
  private final RepositoryPolicy policy;

  private SharedRepository(Path path) {
    this.path = path;
    this.file = path.toFile();
    this.core = new GitRepositoryCore(path, RepositoryPolicy.SHARED);
    this.policy = RepositoryPolicy.SHARED;
  }

  public Path path() {
    return path;
  }

  public boolean hasDotGit() {
    return Files.exists(path.resolve(".git"));
  }

  /** Initializes an empty working-tree repository if necessary. */
  public void initialize() throws IOException, GitAPIException {
    policy.require(RepositoryCapability.WORKTREE);
    core.withLock(
        () -> {
          try (Git ignored = openOrInit()) {
            // Initialization is performed by openOrInit.
          }
          return null;
        });
  }

  private SharedRepository cloneFrom(
      String remoteUri, @Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.WORKTREE);
    policy.require(RepositoryCapability.REMOTES);
    if (remoteUri == null || remoteUri.isBlank()) {
      throw new IllegalArgumentException("Remote URI must not be blank");
    }
    if (Files.exists(path)) {
      throw new IOException("Clone destination already exists: " + path);
    }

    return core.withLock(
        () -> {
          try {
            var command = Git.cloneRepository().setURI(remoteUri).setDirectory(file);
            if (credentialsProvider != null) {
              command.setCredentialsProvider(credentialsProvider);
            }
            try (Git ignored = command.call()) {
              return SharedRepository.this;
            }
          } catch (GitAPIException | RuntimeException e) {
            try {
              NioFileTree.deleteRecursivelyIfExists(path);
            } catch (IOException cleanupFailure) {
              e.addSuppressed(cleanupFailure);
            }
            throw e;
          }
        });
  }

  /** Fetches tracking references from {@code origin}. */
  public int fetch(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.REMOTES);
    return core.withLock(
        () -> {
          try (Git git = requireOpen()) {
            var command = git.fetch().setRemote("origin");
            if (credentialsProvider != null) {
              command.setCredentialsProvider(credentialsProvider);
            }
            return command.call().getTrackingRefUpdates().size();
          }
        });
  }

  /** Pulls from {@code origin}, accepting only a fast-forward update. */
  public String pull(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.REMOTES);
    return core.withLock(
        () -> {
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
        });
  }

  /** Pushes the current branch to its configured upstream. */
  public int push(@Nullable CredentialsProvider credentialsProvider)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.REMOTES);
    return core.withLock(
        () -> {
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
        });
  }

  /**
   * Stages and commits changes below one repository-relative directory.
   *
   * @return the new commit, or an empty result when that directory has no changes
   */
  public Optional<CommitInfo> commit(
      String repositoryRelativePath, String message, GitRepositoryCore.Identity identity)
      throws IOException, GitAPIException {
    return commit(List.of(repositoryRelativePath), message, identity);
  }

  /** Restores selected index entries to {@code HEAD} without changing the working tree. */
  public void resetIndexPaths(Collection<String> repositoryRelativePaths)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.WORKTREE);
    if (repositoryRelativePaths == null || repositoryRelativePaths.isEmpty()) {
      throw new IllegalArgumentException("At least one Git path is required");
    }
    var gitPaths = new LinkedHashSet<String>();
    repositoryRelativePaths.forEach(path -> gitPaths.add(validateGitPath(path)));

    core.withLock(
        () -> {
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
                      .anyMatch(
                          path -> indexedPath.equals(path) || indexedPath.startsWith(path + "/"))) {
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
          return null;
        });
  }

  /** Stages and commits changes below one or more repository-relative paths. */
  public Optional<CommitInfo> commit(
      Collection<String> repositoryRelativePaths,
      String message,
      GitRepositoryCore.Identity identity)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.WORKTREE);
    policy.require(RepositoryCapability.CREATE_COMMIT);
    if (repositoryRelativePaths == null || repositoryRelativePaths.isEmpty()) {
      throw new IllegalArgumentException("At least one Git path is required");
    }
    var gitPaths = new LinkedHashSet<String>();
    repositoryRelativePaths.forEach(path -> gitPaths.add(validateGitPath(path)));
    Objects.requireNonNull(identity, "identity");
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Commit message must not be blank");
    }

    return core.withLock(
        () -> {
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
              return Optional.<CommitInfo>empty();
            }

            var person = new PersonIdent(identity.name(), identity.email());
            var command = git.commit().setMessage(message).setAuthor(person).setCommitter(person);
            gitPaths.forEach(command::setOnly);
            RevCommit commit = command.call();
            return Optional.of(toInfo(commit));
          }
        });
  }

  /** Lists one cursor-based page of newest-first commits for a repository-relative path. */
  public HistoryPage historyPage(
      String repositoryRelativePath, int limit, @Nullable String beforeRevision)
      throws IOException, GitAPIException {
    policy.require(RepositoryCapability.READ_HISTORY);
    String gitPath = validateGitPath(repositoryRelativePath);
    if (limit < 1) {
      throw new IllegalArgumentException("History limit must be positive");
    }
    if (beforeRevision != null && beforeRevision.isBlank()) {
      throw new IllegalArgumentException("History cursor must not be blank");
    }

    return core.withLock(
        () -> {
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
        });
  }

  /** Resolves an advanced revision string to one exact commit object ID. */
  public SnapshotId resolveCommit(String revision) throws IOException {
    policy.require(RepositoryCapability.READ_HISTORY);
    return core.resolveCommit(revision);
  }

  /** Reads one small file from the tree of the resolved revision. */
  public byte[] readSmallFile(String revision, String path, long maxBytes) throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    return core.readResolvedSmallFile(revision, path, maxBytes);
  }

  /** Lists repository-relative file paths present in the resolved revision. */
  public List<String> listFiles(String revision) throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    return core.listResolvedFiles(revision, SnapshotTreeLimits.DEFAULT);
  }

  /**
   * Exports the parcel directory from a commit without changing HEAD, the index, or the working
   * tree.
   *
   * @param destination a path which must not already exist
   */
  public void exportRevision(
      String revision, String repositoryRelativePath, Path destination) throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    String gitPath = validateGitPath(repositoryRelativePath);
    if (revision == null || revision.isBlank()) {
      throw new IllegalArgumentException("Revision must not be blank");
    }
    try {
      core.withLock(
          () -> {
            // The subtree export re-acquires the same reentrant path lock.
            core.exportResolvedSubtree(
                revision,
                gitPath,
                destination,
                SnapshotTreeLimits.DEFAULT,
                ProgressReporter.NONE);
            return null;
          });
    } catch (GitAPIException e) {
      throw new IOException("Unexpected JGit failure for " + path, e);
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

  public record CommitInfo(
      String revision, Instant committedAt, String author, String message) {}

  public record HistoryPage(List<CommitInfo> commits, Optional<String> nextCursor) {
    public HistoryPage {
      commits = List.copyOf(commits);
      nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    }
  }
}
