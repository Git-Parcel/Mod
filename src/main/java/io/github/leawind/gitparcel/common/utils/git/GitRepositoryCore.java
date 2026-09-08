package io.github.leawind.gitparcel.common.utils.git;

import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode.ContentSummary;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

/** Shared JGit primitives for policy-constrained internal and full shared repositories. */
public final class GitRepositoryCore {
  /**
   * One lock per repository path, shared by every core, {@link InternalRepository}, and {@link
   * SharedRepository} instance. Values are weak so entries for abandoned repositories (for example
   * after a parcel is deleted) are collected instead of accumulating forever; an entry stays alive
   * while any instance still references its lock.
   */
  private static final ConcurrentMap<Path, ReentrantLock> LOCKS =
      new com.google.common.collect.MapMaker().weakValues().makeMap();

  private static final com.github.benmanes.caffeine.cache.Cache<SnapshotId, CommitData>
      COMMIT_CACHE =
          com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
              .maximumSize(4096)
              .build();

  private final Path path;
  private final RepositoryPolicy policy;
  private final ReentrantLock lock;

  public GitRepositoryCore(Path path, RepositoryPolicy policy) {
    this.path = path.toAbsolutePath().normalize();
    this.policy = policy;
    this.lock = LOCKS.computeIfAbsent(this.path, ignored -> new ReentrantLock());
    try {
      this.path.toFile();
    } catch (UnsupportedOperationException e) {
      throw new IllegalArgumentException("JGit repositories require the default file system", e);
    }
  }

  public Path path() {
    return path;
  }

  public RepositoryPolicy policy() {
    return policy;
  }

  public boolean exists() {
    return Files.isRegularFile(path.resolve("config"))
        || Files.isRegularFile(path.resolve(".git").resolve("config"));
  }

  public void initializeBare() throws IOException, GitAPIException {
    policy.require(RepositoryCapability.CREATE_COMMIT);
    locked(
        () -> {
          if (exists()) {
            try (Repository repository = open()) {
              if (!repository.isBare()) {
                throw new IOException("Expected a bare Git repository: " + path);
              }
            }
            return null;
          }
          if (Files.exists(path)) {
            try (var entries = Files.list(path)) {
              if (entries.findAny().isPresent()) {
                throw new IOException("Refusing to replace non-empty repository directory: " + path);
              }
            }
          }
          Files.createDirectories(path);
          try (Git ignored = Git.init().setBare(true).setDirectory(path.toFile()).call()) {
            return null;
          }
        });
  }

  public boolean isBare() throws IOException {
    return locked(
        () -> {
          try (Repository repository = open()) {
            return repository.isBare();
          }
        });
  }

  public SnapshotId writeTree(
      Path workspace, SnapshotTreeLimits limits, ProgressReporter progress) throws IOException {
    policy.require(RepositoryCapability.CREATE_COMMIT);
    ProgressReporter reporter = ProgressReporter.safe(progress);
    return locked(
        () -> {
          List<WorkspaceFile> files = inspectWorkspace(workspace, limits);
          reporter.report("git_objects", 0, files.size(), "files");
          try (Repository repository = open(); var inserter = repository.newObjectInserter()) {
            DirCache cache = DirCache.newInCore();
            var builder = cache.builder();
            long completed = 0;
            for (WorkspaceFile file : files) {
              ObjectId blob;
              try (var stream = Files.newInputStream(file.path())) {
                blob = inserter.insert(Constants.OBJ_BLOB, file.size(), stream);
              }
              var entry = new DirCacheEntry(file.gitPath());
              entry.setFileMode(FileMode.REGULAR_FILE);
              entry.setObjectId(blob);
              builder.add(entry);
              reporter.report("git_objects", ++completed, files.size(), "files");
            }
            builder.finish();
            ObjectId tree = cache.writeTree(inserter);
            inserter.flush();
            return new SnapshotId(tree.name());
          }
        });
  }

  public SnapshotId writeSmallTree(Map<String, byte[]> files, SnapshotTreeLimits limits)
      throws IOException {
    policy.require(RepositoryCapability.CREATE_COMMIT);
    return locked(
        () -> {
          var entries = new ArrayList<>(files.entrySet());
          entries.sort(Map.Entry.comparingByKey());
          long total = 0;
          if (entries.size() > limits.maxFiles()) {
            throw new IOException("Snapshot contains too many files");
          }
          var portablePaths = new HashMap<String, String>();
          try (Repository repository = open(); var inserter = repository.newObjectInserter()) {
            DirCache cache = DirCache.newInCore();
            var builder = cache.builder();
            for (var file : entries) {
              SafeSnapshotPath.validateGitPath(file.getKey(), limits.maxDepth());
              registerPortablePath(portablePaths, file.getKey());
              if (file.getValue().length > limits.maxFileBytes()
                  || (total += file.getValue().length) > limits.maxTotalBytes()) {
                throw new IOException("Snapshot content exceeds configured size limits");
              }
              ObjectId blob = inserter.insert(Constants.OBJ_BLOB, file.getValue());
              var entry = new DirCacheEntry(file.getKey());
              entry.setFileMode(FileMode.REGULAR_FILE);
              entry.setObjectId(blob);
              builder.add(entry);
            }
            builder.finish();
            ObjectId tree = cache.writeTree(inserter);
            inserter.flush();
            return new SnapshotId(tree.name());
          }
        });
  }

  public SnapshotId createCommit(
      SnapshotId tree,
      List<SnapshotId> parents,
      String message,
      Identity author,
      Identity committer,
      Instant timestamp)
      throws IOException {
    policy.require(RepositoryCapability.CREATE_COMMIT);
    if (parents.size() > 1) {
      policy.require(RepositoryCapability.MULTI_PARENT_COMMITS);
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Commit message must not be blank");
    }
    return locked(
        () -> {
          try (Repository repository = open(); var inserter = repository.newObjectInserter()) {
            var builder = new CommitBuilder();
            builder.setTreeId(ObjectId.fromString(tree.value()));
            builder.setParentIds(parents.stream().map(id -> ObjectId.fromString(id.value())).toList());
            Date when = Date.from(timestamp);
            TimeZone utc = TimeZone.getTimeZone("UTC");
            builder.setAuthor(new PersonIdent(author.name(), author.email(), when, utc));
            builder.setCommitter(new PersonIdent(committer.name(), committer.email(), when, utc));
            builder.setMessage(message);
            ObjectId commit = inserter.insert(builder);
            inserter.flush();
            return new SnapshotId(commit.name());
          }
        });
  }

  public Optional<SnapshotId> exactRef(String name) throws IOException {
    policy.require(RepositoryCapability.READ_HISTORY);
    return locked(
        () -> {
          try (Repository repository = open()) {
            Ref ref = repository.exactRef(name);
            return ref == null || ref.getObjectId() == null
                ? Optional.empty()
                : Optional.of(new SnapshotId(ref.getObjectId().name()));
          }
        });
  }

  /** Resolves an advanced shared-repository revision to one exact commit object ID. */
  public SnapshotId resolveCommit(String revision) throws IOException {
    policy.require(RepositoryCapability.READ_HISTORY);
    if (revision == null || revision.isBlank()) {
      throw new IllegalArgumentException("Git revision must not be blank");
    }
    return locked(
        () -> {
          try (Repository repository = open()) {
            ObjectId resolved = repository.resolve(revision + "^{commit}");
            if (resolved == null) {
              throw new IOException("Unknown Git revision: " + revision);
            }
            return new SnapshotId(resolved.name());
          }
        });
  }

  public List<NamedRef> refsByPrefix(String prefix) throws IOException {
    policy.require(RepositoryCapability.READ_HISTORY);
    return locked(
        () -> {
          try (Repository repository = open()) {
            return repository.getRefDatabase().getRefsByPrefix(prefix).stream()
                .filter(ref -> ref.getObjectId() != null)
                .map(ref -> new NamedRef(ref.getName(), new SnapshotId(ref.getObjectId().name())))
                .sorted(Comparator.comparing(NamedRef::name))
                .toList();
          }
        });
  }

  public RefUpdate.Result compareAndSetRef(
      String name, Optional<SnapshotId> expected, SnapshotId update, boolean force)
      throws IOException {
    policy.require(RepositoryCapability.UPDATE_MANAGED_REFS);
    return locked(
        () -> {
          try (Repository repository = open()) {
            RefUpdate ref = repository.updateRef(name);
            ref.setExpectedOldObjectId(
                expected.map(id -> ObjectId.fromString(id.value())).orElse(ObjectId.zeroId()));
            ref.setNewObjectId(ObjectId.fromString(update.value()));
            ref.setForceUpdate(force);
            return ref.update();
          }
        });
  }

  public RefUpdate.Result deleteRef(String name, Optional<SnapshotId> expected) throws IOException {
    policy.require(RepositoryCapability.UPDATE_MANAGED_REFS);
    return locked(
        () -> {
          try (Repository repository = open()) {
            RefUpdate ref = repository.updateRef(name);
            expected.ifPresent(id -> ref.setExpectedOldObjectId(ObjectId.fromString(id.value())));
            ref.setForceUpdate(true);
            return ref.delete();
          }
        });
  }

  public RefUpdate.Result linkHead(String target) throws IOException {
    policy.require(RepositoryCapability.UPDATE_MANAGED_REFS);
    return locked(
        () -> {
          try (Repository repository = open()) {
            return repository.updateRef(Constants.HEAD).link(target);
          }
        });
  }

  public CommitData readCommit(SnapshotId id) throws IOException {
    policy.require(RepositoryCapability.READ_HISTORY);
    // Git object IDs are content-addressed, so a parsed commit is immutable and shareable across
    // repositories. Caching keeps health inspections and tree queries from re-reading every
    // commit object on each operation.
    var cached = COMMIT_CACHE.getIfPresent(id);
    if (cached != null) {
      return cached;
    }
    CommitData data =
        locked(
            () -> {
              try (Repository repository = open();
                  var walk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
                var commit = walk.parseCommit(ObjectId.fromString(id.value()));
                walk.parseTree(commit.getTree().getId());
                return new CommitData(
                    id,
                    new SnapshotId(commit.getTree().getId().name()),
                    java.util.Arrays.stream(commit.getParents())
                        .map(parent -> new SnapshotId(parent.getId().name()))
                        .toList(),
                    commit.getFullMessage(),
                    commit.getAuthorIdent().getName(),
                    Instant.ofEpochSecond(commit.getCommitTime()));
              }
            });
    COMMIT_CACHE.put(id, data);
    return data;
  }

  public ContentSummary summarizeTree(SnapshotId treeId) throws IOException {
    return summarizeTree(treeId, SnapshotTreeLimits.DEFAULT);
  }

  public ContentSummary summarizeTree(SnapshotId treeId, SnapshotTreeLimits limits)
      throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    return locked(
        () -> {
          long bytes = 0;
          long files;
          try (Repository repository = open()) {
            List<TreeFile> entries =
                inspectTree(repository, ObjectId.fromString(treeId.value()), limits, null);
            files = entries.size();
            for (TreeFile entry : entries) {
              bytes = Math.addExact(bytes, entry.size());
            }
          }
          return new ContentSummary(files, bytes);
        });
  }

  public byte[] readSmallFile(SnapshotId commitId, String path, long maxBytes) throws IOException {
    return readResolvedSmallFile(commitId.value(), path, maxBytes);
  }

  public byte[] readResolvedSmallFile(String revision, String path, long maxBytes)
      throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    SafeSnapshotPath.validateGitPath(path, 64);
    return locked(
        () -> {
          try (Repository repository = open();
            var revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            ObjectId resolved = repository.resolve(revision + "^{commit}");
            if (resolved == null) {
              throw new IOException("Unknown Git revision: " + revision);
            }
            try (var treeWalk =
                TreeWalk.forPath(repository, path, revWalk.parseCommit(resolved).getTree())) {
            if (treeWalk == null || !FileMode.REGULAR_FILE.equals(treeWalk.getFileMode(0))) {
              throw new IOException("Commit does not contain regular file " + path);
            }
            var loader = repository.open(treeWalk.getObjectId(0));
            if (loader.getSize() > maxBytes) {
              throw new IOException("Git file exceeds configured size limit: " + path);
            }
            return loader.getCachedBytes((int) Math.min(Integer.MAX_VALUE, maxBytes));
            }
          }
        });
  }

  public void exportCommitTree(
      SnapshotId commitId,
      Path destination,
      SnapshotTreeLimits limits,
      ProgressReporter progress)
      throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    exportResolvedCommit(commitId.value(), destination, limits, progress);
  }

  /** Shared repositories may intentionally address a ref; internal callers use {@link SnapshotId}. */
  public void exportResolvedCommit(
      String revision,
      Path destination,
      SnapshotTreeLimits limits,
      ProgressReporter progress)
      throws IOException {
    exportResolved(revision, null, destination, limits, progress);
  }

  public void exportResolvedSubtree(
      String revision,
      String subtree,
      Path destination,
      SnapshotTreeLimits limits,
      ProgressReporter progress)
      throws IOException {
    exportResolved(
        revision,
        SafeSnapshotPath.validateGitPath(subtree, limits.maxDepth()),
        destination,
        limits,
        progress);
  }

  public List<String> listResolvedFiles(String revision, SnapshotTreeLimits limits)
      throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    return locked(
        () -> {
          try (Repository repository = open();
              var revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            ObjectId resolved = repository.resolve(revision + "^{commit}");
            if (resolved == null) {
              throw new IOException("Unknown Git revision: " + revision);
            }
            return inspectTree(
                    repository,
                    revWalk.parseCommit(resolved).getTree().getId(),
                    limits,
                    null)
                .stream()
                .map(TreeFile::path)
                .toList();
          }
        });
  }

  private void exportResolved(
      String revision,
      String subtree,
      Path destination,
      SnapshotTreeLimits limits,
      ProgressReporter progress)
      throws IOException {
    policy.require(RepositoryCapability.READ_TREE);
    ProgressReporter reporter = ProgressReporter.safe(progress);
    locked(
        () -> {
          if (Files.exists(destination)) {
            throw new IOException("Export destination already exists: " + destination);
          }
          try (Repository repository = open(); var revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
            ObjectId resolved = repository.resolve(revision + "^{commit}");
            if (resolved == null) {
              throw new IOException("Unknown Git revision: " + revision);
            }
            var commit = revWalk.parseCommit(resolved);
            List<TreeFile> entries =
                inspectTree(repository, commit.getTree().getId(), limits, subtree);
            if (entries.isEmpty()) {
              throw new IOException(
                  subtree == null
                      ? "Snapshot tree contains no regular files"
                      : "Revision does not contain snapshot path " + subtree);
            }
            reporter.report("git_tree_read", 0, entries.size(), "files");
            Files.createDirectories(destination);
            long completed = 0;
            try {
              for (TreeFile entry : entries) {
                Path output = SafeSnapshotPath.resolve(destination, entry.path(), limits.maxDepth());
                Files.createDirectories(output.getParent());
                try (OutputStream stream = Files.newOutputStream(output)) {
                  repository.open(entry.id()).copyTo(stream);
                }
                reporter.report("git_tree_read", ++completed, entries.size(), "files");
              }
            } catch (IOException | RuntimeException e) {
              deleteRecursively(destination);
              throw e;
            }
          }
          return null;
        });
  }

  private Repository open() throws IOException {
    if (!exists()) {
      throw new IOException("Git repository does not exist: " + path);
    }
    Path gitDir = Files.isRegularFile(path.resolve("config")) ? path : path.resolve(".git");
    return new FileRepositoryBuilder().setGitDir(gitDir.toFile()).setMustExist(true).build();
  }

  private static List<WorkspaceFile> inspectWorkspace(Path workspace, SnapshotTreeLimits limits)
      throws IOException {
    if (!Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Snapshot workspace is not a directory: " + workspace);
    }
    Path root = workspace.normalize();
    var files = new ArrayList<WorkspaceFile>();
    var fileKeys = new HashSet<Object>();
    var portablePaths = new HashMap<String, String>();
    final long[] total = {0};
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            if (attrs.isSymbolicLink() || !attrs.isDirectory()) {
              throw new IOException("Snapshot contains a non-directory tree entry: " + dir);
            }
            if (!dir.equals(root)) {
              registerPortablePath(
                  portablePaths,
                  SafeSnapshotPath.toGitPath(root, dir, limits.maxDepth()));
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isSymbolicLink() || !attrs.isRegularFile()) {
              throw new IOException("Snapshot contains a special file: " + file);
            }
            if (attrs.fileKey() != null && !fileKeys.add(attrs.fileKey())) {
              throw new IOException("Snapshot contains hard-linked files: " + file);
            }
            if (attrs.size() > limits.maxFileBytes()) {
              throw new IOException("Snapshot file is too large: " + file);
            }
            total[0] = Math.addExact(total[0], attrs.size());
            if (total[0] > limits.maxTotalBytes()) {
              throw new IOException("Snapshot expanded size exceeds configured limit");
            }
            if (files.size() >= limits.maxFiles()) {
              throw new IOException("Snapshot contains too many files");
            }
            String gitPath = SafeSnapshotPath.toGitPath(root, file, limits.maxDepth());
            registerPortablePath(portablePaths, gitPath);
            files.add(new WorkspaceFile(file, gitPath, attrs.size()));
            return FileVisitResult.CONTINUE;
          }
        });
    files.sort(Comparator.comparing(WorkspaceFile::gitPath));
    return files;
  }

  private static List<TreeFile> inspectTree(
      Repository repository, ObjectId tree, SnapshotTreeLimits limits, String subtree)
      throws IOException {
    var entries = new ArrayList<TreeFile>();
    var portablePaths = new HashMap<String, String>();
    long total = 0;
    try (var walk = new TreeWalk(repository)) {
      walk.addTree(tree);
      walk.setRecursive(true);
      while (walk.next()) {
        String treePath = walk.getPathString();
        if (subtree != null && !treePath.startsWith(subtree + "/")) {
          continue;
        }
        String path =
            SafeSnapshotPath.validateGitPath(
                subtree == null ? treePath : treePath.substring(subtree.length() + 1),
                limits.maxDepth());
        registerPortablePath(portablePaths, path);
        if (!FileMode.REGULAR_FILE.equals(walk.getFileMode(0))) {
          throw new IOException("Unsupported Git tree mode for snapshot file: " + path);
        }
        long size = repository.open(walk.getObjectId(0)).getSize();
        if (size > limits.maxFileBytes()) {
          throw new IOException("Snapshot file is too large: " + path);
        }
        total = Math.addExact(total, size);
        if (total > limits.maxTotalBytes() || entries.size() >= limits.maxFiles()) {
          throw new IOException("Snapshot tree exceeds configured limits");
        }
        entries.add(new TreeFile(path, walk.getObjectId(0).copy(), size));
      }
    }
    return entries;
  }

  private static void registerPortablePath(Map<String, String> paths, String path)
      throws IOException {
    StringBuilder prefix = new StringBuilder();
    for (String part : path.split("/")) {
      if (!prefix.isEmpty()) {
        prefix.append('/');
      }
      prefix.append(part);
      String original = prefix.toString();
      String previous = paths.putIfAbsent(original.toLowerCase(Locale.ROOT), original);
      if (previous != null && !previous.equals(original)) {
        throw new IOException(
            "Snapshot contains platform-ambiguous paths: " + previous + " and " + original);
      }
    }
  }

  private static void deleteRecursively(Path root) throws IOException {
    NioFileTree.deleteRecursivelyIfExists(root);
  }

  private <T> T locked(LockedAction<T> action) throws IOException {
    try {
      return withLock(action);
    } catch (GitAPIException e) {
      throw new IOException("Git operation failed for " + path, e);
    }
  }

  /**
   * Runs an action while holding this repository's path lock, making multi-step sequences atomic
   * against other users of the same repository. Nesting on one thread is reentrant.
   */
  public <T> T withLock(LockedAction<T> action) throws IOException, GitAPIException {
    lock.lock();
    try {
      return action.run();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Runs an action after acquiring the path lock within the given timeout. Read-style access must
   * use this entry point so a caller that ignores threading contracts fails fast with {@link
   * RepositoryBusyException} instead of blocking a server tick indefinitely.
   */
  public <T> T tryWithLock(long timeout, TimeUnit unit, LockedAction<T> action)
      throws IOException, GitAPIException {
    boolean acquired;
    try {
      acquired = lock.tryLock(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while acquiring repository access: " + path, e);
    }
    if (!acquired) {
      throw new RepositoryBusyException(
          "Repository is busy with a long operation; retry later: " + path);
    }
    try {
      return action.run();
    } finally {
      lock.unlock();
    }
  }

  /** An action run under this repository's path lock. */
  @FunctionalInterface
  public interface LockedAction<T> {
    T run() throws IOException, GitAPIException;
  }

  private record WorkspaceFile(Path path, String gitPath, long size) {}

  private record TreeFile(String path, ObjectId id, long size) {}

  public record Identity(String name, String email) {
    public Identity {
      if (name == null || name.isBlank() || email == null || email.isBlank()) {
        throw new IllegalArgumentException("Git identity name and email must not be blank");
      }
    }
  }

  public record NamedRef(String name, SnapshotId target) {}

  public record CommitData(
      SnapshotId id,
      SnapshotId treeId,
      List<SnapshotId> parents,
      String message,
      String author,
      Instant committedAt) {
    public CommitData {
      parents = List.copyOf(parents);
    }
  }
}
