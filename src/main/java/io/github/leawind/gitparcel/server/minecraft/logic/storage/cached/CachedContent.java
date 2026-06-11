package io.github.leawind.gitparcel.server.minecraft.logic.storage.cached;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Manages the download cache for packaged parcels. */
public final class CachedContent {
  /** Directory name for cached content. */
  public static final String CACHED_DIR_NAME = "cached";

  private final Path root;

  /**
   * Creates a CachedContent manager for the specified root directory.
   *
   * @param root the cache root directory
   */
  public CachedContent(Path root) {
    this.root = root;
  }

  /**
   * Gets the cache root directory.
   *
   * @return the cache directory path
   */
  public Path getRoot() {
    return root;
  }

  /**
   * Gets or creates the directory for a download source.
   *
   * @param sourceId the download source identifier (e.g. "github")
   * @return the source directory path
   * @throws IOException if the directory cannot be created
   */
  public Path getSourceDir(String sourceId) throws IOException {
    var dir = root.resolve(sourceId);
    Files.createDirectories(dir);
    return dir;
  }
}
