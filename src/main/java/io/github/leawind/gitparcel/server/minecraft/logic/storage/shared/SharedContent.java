package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages shareable content that can be distributed across game instances.
 *
 * <p>Shared content consists of parcel repositories (git repos) stored in a common directory. Each
 * repo is tracked via {@code repos.json} at the shared root, and each repo has a {@code meta.json}
 * mapping the relative paths of parcels within it.
 */
public final class SharedContent {
  private static final Logger LOGGER = LoggerFactory.getLogger(SharedContent.class);

  /** Directory name for shared content. */
  public static final String SHARED_DIR_NAME = "shared";

  /** Repository index file name. */
  private static final String REPOS_INDEX_FILE = "repos.json";

  /** Repository metadata file name (within each repo directory). */
  private static final String REPO_META_FILE = "meta.json";

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final Pattern REPOSITORY_NAME =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

  private final Path root;

  /**
   * Creates a SharedContent manager for the specified root directory.
   *
   * @param root the shared content root directory
   */
  public SharedContent(Path root) {
    this.root = root.toAbsolutePath().normalize();
  }

  /**
   * Gets the shared content root directory.
   *
   * @return the shared content directory path
   */
  public Path getRoot() {
    return root;
  }

  /**
   * Gets the path to {@code repos.json}, the repository index file.
   *
   * @return the repos.json path
   */
  public Path getReposIndexFile() {
    return root.resolve(REPOS_INDEX_FILE);
  }

  /**
   * Loads the repository index from {@code repos.json}.
   *
   * @return map from repo name to repo info, empty map if the file does not exist
   * @throws IOException if the file exists but cannot be read
   */
  public synchronized Map<String, RepoInfo> loadReposIndex() throws IOException {
    var file = getReposIndexFile();
    if (!Files.exists(file)) {
      return Collections.emptyMap();
    }
    try {
      var json = GSON.fromJson(Files.readString(file), JsonObject.class);
      if (json == null) {
        throw new IOException("Repository index must contain a JSON object: " + file);
      }
      var repos = json.getAsJsonObject("repos");
      if (repos == null) {
        return Collections.emptyMap();
      }
      Type type = new TypeToken<Map<String, RepoInfo>>() {}.getType();
      Map<String, RepoInfo> result = GSON.fromJson(repos, type);
      if (result == null) {
        return Collections.emptyMap();
      }
      result.forEach(
          (name, info) -> {
            validateRepositoryName(name);
            if (info == null) {
              throw new IllegalArgumentException(
                  "Missing repository information for " + name);
            }
          });
      return Map.copyOf(result);
    } catch (RuntimeException e) {
      throw new IOException("Invalid shared repository index: " + file, e);
    }
  }

  /**
   * Saves the repository index to {@code repos.json}.
   *
   * @param repos map from repo name to repo info
   * @throws IOException if an I/O error occurs
   */
  public synchronized void saveReposIndex(Map<String, RepoInfo> repos) throws IOException {
    repos.forEach(
        (name, info) -> {
          validateRepositoryName(name);
          if (info == null) {
            throw new IllegalArgumentException(
                "Missing repository information for " + name);
          }
        });
    var json = new JsonObject();
    json.add("repos", GSON.toJsonTree(repos));
    writeAtomically(getReposIndexFile(), GSON.toJson(json));
  }

  public synchronized Optional<RepoInfo> getRepository(String repoName) throws IOException {
    validateRepositoryName(repoName);
    return Optional.ofNullable(loadReposIndex().get(repoName));
  }

  /** Adds a repository without overwriting an existing catalog entry. */
  public synchronized void addRepository(String repoName, RepoInfo info) throws IOException {
    validateRepositoryName(repoName);
    if (info == null) {
      throw new IllegalArgumentException("Repository information must not be null");
    }
    var repos = new HashMap<>(loadReposIndex());
    if (repos.putIfAbsent(repoName, info) != null) {
      throw new IOException("Shared repository already exists: " + repoName);
    }
    saveReposIndex(repos);
  }

  /** Atomically replaces one existing repository's catalog information. */
  public synchronized RepoInfo updateRepository(
      String repoName, UnaryOperator<RepoInfo> updater) throws IOException {
    validateRepositoryName(repoName);
    var repos = new HashMap<>(loadReposIndex());
    RepoInfo previous = repos.get(repoName);
    if (previous == null) {
      throw new IOException("Unknown shared repository: " + repoName);
    }
    RepoInfo updated = updater.apply(previous);
    if (updated == null) {
      throw new IllegalArgumentException("Repository update must not return null");
    }
    repos.put(repoName, updated);
    saveReposIndex(repos);
    return updated;
  }

  /**
   * Loads the parcel list from the repo's {@code meta.json}.
   *
   * @param repoName the repository name (directory name under shared root)
   * @return list of parcel relative paths within the repo, empty list if the file does not exist
   * @throws IOException if the file exists but cannot be read
   */
  public synchronized List<String> loadRepoMeta(String repoName) throws IOException {
    var metaFile = getRepoMetaFile(repoName);
    if (!Files.exists(metaFile)) {
      return Collections.emptyList();
    }
    try {
      var json = GSON.fromJson(Files.readString(metaFile), JsonObject.class);
      if (json == null) {
        throw new IOException("Repository metadata must contain a JSON object: " + metaFile);
      }
      var parcels = json.getAsJsonArray("parcels");
      if (parcels == null) {
        return Collections.emptyList();
      }
      Type type = new TypeToken<List<String>>() {}.getType();
      List<String> result = GSON.fromJson(parcels, type);
      if (result == null) {
        return Collections.emptyList();
      }
      result.forEach(path -> validateParcelPath(repoName, path));
      return List.copyOf(result);
    } catch (RuntimeException e) {
      throw new IOException("Invalid shared repository metadata: " + metaFile, e);
    }
  }

  /**
   * Saves the parcel list to the repo's {@code meta.json}.
   *
   * @param repoName the repository name
   * @param parcelPaths list of parcel relative paths within the repo
   * @throws IOException if an I/O error occurs
   */
  public synchronized void saveRepoMeta(String repoName, List<String> parcelPaths)
      throws IOException {
    validateRepositoryName(repoName);
    parcelPaths.forEach(path -> validateParcelPath(repoName, path));
    var metaFile = getRepoMetaFile(repoName);
    var json = new JsonObject();
    json.add("parcels", GSON.toJsonTree(parcelPaths));
    writeAtomically(metaFile, GSON.toJson(json));
  }

  public synchronized boolean containsParcelPath(String repoName, String parcelPath)
      throws IOException {
    String expected = getParcelGitPath(repoName, parcelPath);
    return loadRepoMeta(repoName).stream()
        .map(path -> getParcelGitPath(repoName, path))
        .anyMatch(expected::equals);
  }

  /** Adds a normalized parcel path to repository metadata. */
  public synchronized void addParcelPath(String repoName, String parcelPath)
      throws IOException {
    var paths = new TreeSet<String>();
    loadRepoMeta(repoName).stream()
        .map(path -> getParcelGitPath(repoName, path))
        .forEach(paths::add);
    paths.add(getParcelGitPath(repoName, parcelPath));
    saveRepoMeta(repoName, List.copyOf(paths));
  }

  /**
   * Gets the path to a repository directory.
   *
   * @param repoName the repository name
   * @return the repository directory path
   */
  public Path getRepoDir(String repoName) {
    validateRepositoryName(repoName);
    return root.resolve(repoName).normalize();
  }

  /** Resolves and normalizes a parcel directory inside a shared repository. */
  public Path getParcelDir(String repoName, String parcelPath) {
    Path relative = normalizedParcelPath(repoName, parcelPath);
    return getRepoDir(repoName).resolve(relative).normalize();
  }

  /** Returns a validated parcel path using Git's separator. */
  public String getParcelGitPath(String repoName, String parcelPath) {
    Path relative = normalizedParcelPath(repoName, parcelPath);
    return StreamSupport.stream(relative.spliterator(), false)
        .map(Path::toString)
        .collect(Collectors.joining("/"));
  }

  /**
   * Gets the path to a repo's {@code meta.json}.
   *
   * @param repoName the repository name
   * @return the meta.json path
   */
  private Path getRepoMetaFile(String repoName) {
    return getRepoDir(repoName).resolve(REPO_META_FILE);
  }

  public static String validateRepositoryName(String repoName) {
    if (repoName == null || !REPOSITORY_NAME.matcher(repoName).matches()) {
      throw new IllegalArgumentException(
          "Repository name must match " + REPOSITORY_NAME.pattern());
    }
    return repoName;
  }

  private void validateParcelPath(String repoName, String parcelPath) {
    normalizedParcelPath(repoName, parcelPath);
  }

  private Path normalizedParcelPath(String repoName, String parcelPath) {
    validateRepositoryName(repoName);
    if (parcelPath == null || parcelPath.isBlank()) {
      throw new IllegalArgumentException("Parcel path must not be blank");
    }
    Path relative = root.getFileSystem().getPath(parcelPath).normalize();
    if (relative.isAbsolute()
        || relative.toString().isEmpty()
        || relative.startsWith("..")) {
      throw new IllegalArgumentException(
          "Parcel path must stay inside repository " + repoName + ": " + parcelPath);
    }
    for (Path part : relative) {
      if (part.toString().equals(".git")) {
        throw new IllegalArgumentException("Parcel path must not contain .git");
      }
    }
    if (relative.getName(0).toString().equals(REPO_META_FILE)) {
      throw new IllegalArgumentException("Parcel path conflicts with repository metadata");
    }
    return relative;
  }

  private static void writeAtomically(Path file, String content) throws IOException {
    Files.createDirectories(file.getParent());
    Path temporary =
        Files.createTempFile(file.getParent(), "." + file.getFileName() + ".", ".tmp");
    try {
      Files.writeString(temporary, content);
      try {
        Files.move(
            temporary,
            file,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  /**
   * Repository information stored in {@code repos.json}.
   *
   * @param type "cloned" or "local"
   * @param remoteUrl remote URL (only for "cloned" type)
   * @param lastSync ISO 8601 timestamp of last sync (only for "cloned" type)
   */
  public record RepoInfo(String type, @Nullable String remoteUrl, @Nullable String lastSync) {

    private static final String TYPE_CLONED = "cloned";
    private static final String TYPE_LOCAL = "local";

    public RepoInfo {
      if (!TYPE_LOCAL.equals(type) && !TYPE_CLONED.equals(type)) {
        throw new IllegalArgumentException("Unknown shared repository type: " + type);
      }
      if (TYPE_CLONED.equals(type) && (remoteUrl == null || remoteUrl.isBlank())) {
        throw new IllegalArgumentException("Cloned repository must have a remote URL");
      }
      if (lastSync != null) {
        Instant.parse(lastSync);
      }
    }

    /**
     * Creates info for a locally-created repository.
     *
     * @return a new RepoInfo with type "local"
     */
    public static RepoInfo local() {
      return new RepoInfo(TYPE_LOCAL, null, null);
    }

    /**
     * Creates info for a cloned repository.
     *
     * @param remoteUrl the remote URL
     * @return a new RepoInfo with type "cloned"
     */
    public static RepoInfo cloned(String remoteUrl) {
      return new RepoInfo(TYPE_CLONED, remoteUrl, Instant.now().toString());
    }

    public boolean isCloned() {
      return TYPE_CLONED.equals(type);
    }

    public RepoInfo syncedNow() {
      return isCloned()
          ? new RepoInfo(type, remoteUrl, Instant.now().toString())
          : this;
    }
  }
}
