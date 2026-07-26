package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.common.utils.git.SnapshotTreeLimits;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.server.MinecraftServer;
import org.eclipse.jgit.api.errors.GitAPIException;

/** Coordinates catalog and Git operations for repositories in the shared content directory. */
public final class SharedRepositoryService {
  private static final ConcurrentHashMap<Path, SharedRepositoryService> CACHE =
      new ConcurrentHashMap<>();

  public static SharedRepositoryService get(MinecraftServer server) throws IOException {
    Path root = StorageUtils.getSharedDir(server).toAbsolutePath().normalize();
    return CACHE.computeIfAbsent(root, SharedRepositoryService::new);
  }

  private final SharedContent content;
  private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks =
      new ConcurrentHashMap<>();

  public SharedRepositoryService(Path root) {
    this.content = new SharedContent(root);
  }

  public SharedContent content() {
    return content;
  }

  public Map<String, SharedContent.RepoInfo> list() throws IOException {
    return Collections.unmodifiableMap(new TreeMap<>(content.loadReposIndex()));
  }

  public Path repositoryPath(String name) throws IOException {
    requireRegistered(name);
    return content.getRepoDir(name);
  }

  public List<String> parcelPaths(String name) throws IOException {
    requireRegistered(name);
    return content.loadRepoMeta(name);
  }

  /** Reads the portable manifest from one explicit commit instead of trusting the working tree. */
  public List<String> parcelPaths(String name, String revision) throws IOException {
    requireRegistered(name);
    byte[] manifest =
        GitRepo.get(content.getRepoDir(name))
            .core()
            .readResolvedSmallFile(
                revision, SharedContent.REPOSITORY_MANIFEST_FILE, 1024 * 1024);
    List<String> paths = content.parseRepositoryManifest(
        name, new String(manifest, StandardCharsets.UTF_8));
    var declared = new TreeSet<>(paths);
    var actual = new TreeSet<String>();
    for (String file :
        GitRepo.get(content.getRepoDir(name))
            .core()
            .listResolvedFiles(revision, SnapshotTreeLimits.DEFAULT)) {
      if (file.equals("parcel.json")) {
        actual.add("");
      } else if (file.endsWith("/parcel.json")) {
        actual.add(file.substring(0, file.length() - "/parcel.json".length()));
      }
    }
    if (!actual.equals(declared)) {
      throw new IOException(
          "Repository manifest does not match parcel trees; declared="
              + declared
              + ", actual="
              + actual);
    }
    return paths;
  }

  public void create(String name) throws IOException, GitAPIException {
    SharedContent.validateRepositoryName(name);
    var lock = lock(name);
    lock.lock();
    try {
      ensureAvailable(name);
      Path repository = content.getRepoDir(name);
      try {
        GitRepo.get(repository).initialize();
        content.addRepository(name, SharedContent.RepoInfo.local());
      } catch (IOException | GitAPIException | RuntimeException e) {
        cleanupCreatedDirectory(repository, e);
        throw e;
      }
    } finally {
      lock.unlock();
    }
  }

  public void cloneRepository(String name, String remoteUrl)
      throws IOException, GitAPIException {
    SharedContent.validateRepositoryName(name);
    String validatedUrl = GitRemoteAccess.validateRemoteUrl(remoteUrl);
    var lock = lock(name);
    lock.lock();
    try {
      ensureAvailable(name);
      Path repository = content.getRepoDir(name);
      try {
        GitRepo.cloneRepository(
            validatedUrl,
            repository,
            GitRemoteAccess.credentialsFromEnvironment());
        content.addRepository(name, SharedContent.RepoInfo.cloned(validatedUrl));
      } catch (IOException | GitAPIException | RuntimeException e) {
        cleanupCreatedDirectory(repository, e);
        throw e;
      }
    } finally {
      lock.unlock();
    }
  }

  public int fetch(String name) throws IOException, GitAPIException {
    var lock = lock(name);
    lock.lock();
    try {
      requireCloned(name);
      int updates =
          GitRepo.get(content.getRepoDir(name))
              .fetch(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return updates;
    } finally {
      lock.unlock();
    }
  }

  public String pull(String name) throws IOException, GitAPIException {
    var lock = lock(name);
    lock.lock();
    try {
      requireCloned(name);
      String result =
          GitRepo.get(content.getRepoDir(name))
              .pull(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return result;
    } finally {
      lock.unlock();
    }
  }

  public int push(String name) throws IOException, GitAPIException {
    var lock = lock(name);
    lock.lock();
    try {
      requireCloned(name);
      int updates =
          GitRepo.get(content.getRepoDir(name))
              .push(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return updates;
    } finally {
      lock.unlock();
    }
  }

  /** Acquires exclusive access shared with all remote synchronization operations. */
  public RepositoryLease acquire(String name) throws IOException {
    SharedContent.validateRepositoryName(name);
    var lock = lock(name);
    lock.lock();
    try {
      requireRegistered(name);
      return new RepositoryLease(name, content.getRepoDir(name), lock);
    } catch (IOException | RuntimeException e) {
      lock.unlock();
      throw e;
    }
  }

  /** Attempts to acquire a repository without blocking the Minecraft server thread. */
  public Optional<RepositoryLease> tryAcquire(String name) throws IOException {
    SharedContent.validateRepositoryName(name);
    var lock = lock(name);
    if (!lock.tryLock()) {
      return Optional.empty();
    }
    try {
      requireRegistered(name);
      return Optional.of(new RepositoryLease(name, content.getRepoDir(name), lock));
    } catch (IOException | RuntimeException e) {
      lock.unlock();
      throw e;
    }
  }

  private ReentrantLock lock(String name) {
    return repositoryLocks.computeIfAbsent(name, ignored -> new ReentrantLock());
  }

  private void ensureAvailable(String name) throws IOException {
    if (content.getRepository(name).isPresent()) {
      throw new IOException("Shared repository already exists: " + name);
    }
    Path repository = content.getRepoDir(name);
    if (Files.exists(repository)) {
      throw new IOException("Shared repository directory already exists: " + repository);
    }
  }

  private SharedContent.RepoInfo requireRegistered(String name) throws IOException {
    SharedContent.validateRepositoryName(name);
    var info =
        content
            .getRepository(name)
            .orElseThrow(() -> new IOException("Unknown shared repository: " + name));
    Path repository = content.getRepoDir(name);
    if (!GitRepo.get(repository).hasDotGit()) {
      throw new IOException("Shared repository is missing or invalid: " + repository);
    }
    return info;
  }

  private void requireCloned(String name) throws IOException {
    if (!requireRegistered(name).isCloned()) {
      throw new IOException("Shared repository has no managed remote: " + name);
    }
  }

  private void markSynced(String name) throws IOException {
    content.updateRepository(name, SharedContent.RepoInfo::syncedNow);
  }

  private static void cleanupCreatedDirectory(Path directory, Exception original) {
    try {
      NioFileTree.deleteRecursivelyIfExists(directory);
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
  }

  public final class RepositoryLease implements AutoCloseable {
    private final String name;
    private final Path repository;
    private final ReentrantLock lock;
    private boolean closed;

    private RepositoryLease(String name, Path repository, ReentrantLock lock) {
      this.name = name;
      this.repository = repository;
      this.lock = lock;
    }

    public String name() {
      return name;
    }

    public Path repository() {
      return repository;
    }

    public SharedContent content() {
      return content;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        lock.unlock();
      }
    }
  }
}
