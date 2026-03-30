package io.github.leawind.gitparcel.storage.cached;

import java.nio.file.Path;

/**
 * Manages the download cache for packaged parcels.
 *
 * <p>Environment variable {@code GITPARCEL_CACHE_DIR} has the highest priority and overrides the
 * default cache directory.
 */
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
}
