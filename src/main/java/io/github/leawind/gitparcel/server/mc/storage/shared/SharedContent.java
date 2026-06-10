package io.github.leawind.gitparcel.server.mc.storage.shared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

  private final Path root;

  /**
   * Creates a SharedContent manager for the specified root directory.
   *
   * @param root the shared content root directory
   */
  public SharedContent(Path root) {
    this.root = root;
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
  public Map<String, RepoInfo> loadReposIndex() throws IOException {
    var file = getReposIndexFile();
    if (!Files.exists(file)) {
      return Collections.emptyMap();
    }
    var json = GSON.fromJson(Files.readString(file), JsonObject.class);
    var repos = json.getAsJsonObject("repos");
    if (repos == null) {
      return Collections.emptyMap();
    }
    Type type = new TypeToken<Map<String, RepoInfo>>() {}.getType();
    return GSON.fromJson(repos, type);
  }

  /**
   * Saves the repository index to {@code repos.json}.
   *
   * @param repos map from repo name to repo info
   * @throws IOException if an I/O error occurs
   */
  public void saveReposIndex(Map<String, RepoInfo> repos) throws IOException {
    Files.createDirectories(root);
    var json = new JsonObject();
    json.add("repos", GSON.toJsonTree(repos));
    Files.writeString(getReposIndexFile(), GSON.toJson(json));
  }

  /**
   * Loads the parcel list from the repo's {@code meta.json}.
   *
   * @param repoName the repository name (directory name under shared root)
   * @return list of parcel relative paths within the repo, empty list if the file does not exist
   * @throws IOException if the file exists but cannot be read
   */
  public List<String> loadRepoMeta(String repoName) throws IOException {
    var metaFile = getRepoMetaFile(repoName);
    if (!Files.exists(metaFile)) {
      return Collections.emptyList();
    }
    var json = GSON.fromJson(Files.readString(metaFile), JsonObject.class);
    var parcels = json.getAsJsonArray("parcels");
    if (parcels == null) {
      return Collections.emptyList();
    }
    Type type = new TypeToken<List<String>>() {}.getType();
    return GSON.fromJson(parcels, type);
  }

  /**
   * Saves the parcel list to the repo's {@code meta.json}.
   *
   * @param repoName the repository name
   * @param parcelPaths list of parcel relative paths within the repo
   * @throws IOException if an I/O error occurs
   */
  public void saveRepoMeta(String repoName, List<String> parcelPaths) throws IOException {
    var metaFile = getRepoMetaFile(repoName);
    Files.createDirectories(metaFile.getParent());
    var json = new JsonObject();
    json.add("parcels", GSON.toJsonTree(parcelPaths));
    Files.writeString(metaFile, GSON.toJson(json));
  }

  /**
   * Gets the path to a repository directory.
   *
   * @param repoName the repository name
   * @return the repository directory path
   */
  public Path getRepoDir(String repoName) {
    return root.resolve(repoName);
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
  }
}
