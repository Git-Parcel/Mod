package io.github.leawind.gitparcel.common.impl.snapshot;

import io.github.leawind.gitparcel.common.api.snapshot.SnapshotWorkspaceFactory;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Default-filesystem workspace used for one snapshot operation. */
public final class TemporarySnapshotWorkspaceFactory implements SnapshotWorkspaceFactory {
  public static final TemporarySnapshotWorkspaceFactory INSTANCE =
      new TemporarySnapshotWorkspaceFactory();

  private TemporarySnapshotWorkspaceFactory() {}

  @Override
  public Workspace create() throws IOException {
    Path root = Files.createTempDirectory("gitparcel-snapshot-");
    return new Workspace() {
      @Override
      public Path root() {
        return root;
      }

      @Override
      public void close() throws IOException {
        NioFileTree.deleteRecursivelyIfExists(root);
      }
    };
  }
}
