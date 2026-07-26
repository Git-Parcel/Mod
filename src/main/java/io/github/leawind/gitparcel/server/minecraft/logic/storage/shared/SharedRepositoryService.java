package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
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
  private final ConcurrentHashMap<String, Object> repositoryLocks =
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

  public void create(String name) throws IOException, GitAPIException {
    SharedContent.validateRepositoryName(name);
    synchronized (lock(name)) {
      ensureAvailable(name);
      Path repository = content.getRepoDir(name);
      try {
        GitRepo.get(repository).initialize();
        content.addRepository(name, SharedContent.RepoInfo.local());
      } catch (IOException | GitAPIException | RuntimeException e) {
        cleanupCreatedDirectory(repository, e);
        throw e;
      }
    }
  }

  public void cloneRepository(String name, String remoteUrl)
      throws IOException, GitAPIException {
    SharedContent.validateRepositoryName(name);
    String validatedUrl = GitRemoteAccess.validateRemoteUrl(remoteUrl);
    synchronized (lock(name)) {
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
    }
  }

  public int fetch(String name) throws IOException, GitAPIException {
    synchronized (lock(name)) {
      requireCloned(name);
      int updates =
          GitRepo.get(content.getRepoDir(name))
              .fetch(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return updates;
    }
  }

  public String pull(String name) throws IOException, GitAPIException {
    synchronized (lock(name)) {
      requireCloned(name);
      String result =
          GitRepo.get(content.getRepoDir(name))
              .pull(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return result;
    }
  }

  public int push(String name) throws IOException, GitAPIException {
    synchronized (lock(name)) {
      requireCloned(name);
      int updates =
          GitRepo.get(content.getRepoDir(name))
              .push(GitRemoteAccess.credentialsFromEnvironment());
      markSynced(name);
      return updates;
    }
  }

  private Object lock(String name) {
    return repositoryLocks.computeIfAbsent(name, ignored -> new Object());
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
      if (!Files.exists(directory)) {
        return;
      }
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    } catch (IOException cleanupFailure) {
      original.addSuppressed(cleanupFailure);
    }
  }
}
