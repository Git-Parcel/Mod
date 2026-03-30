package io.github.leawind.gitparcel.storage.shared;

import java.nio.file.Path;

/**
 * Manages shareable content that can be distributed across game instances.
 *
 * <p>Environment variable {@code GITPARCEL_SHARE_DIR} has the highest priority and overrides the
 * default shared directory.
 */
public final class SharedContent {
  /** Directory name for shared content. */
  public static final String SHARED_DIR_NAME = "shared";

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
}
