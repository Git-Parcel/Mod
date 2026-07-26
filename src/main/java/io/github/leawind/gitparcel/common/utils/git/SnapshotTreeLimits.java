package io.github.leawind.gitparcel.common.utils.git;

/** Expanded snapshot-tree safety limits. */
public record SnapshotTreeLimits(long maxFiles, long maxFileBytes, long maxTotalBytes, int maxDepth) {
  public static final SnapshotTreeLimits DEFAULT =
      new SnapshotTreeLimits(100_000, 1L << 30, 8L << 30, 64);

  public SnapshotTreeLimits {
    if (maxFiles < 1 || maxFileBytes < 1 || maxTotalBytes < 1 || maxDepth < 1) {
      throw new IllegalArgumentException("Snapshot tree limits must be positive");
    }
  }
}
