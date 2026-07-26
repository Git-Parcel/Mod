package io.github.leawind.gitparcel.common.api.snapshot;

import java.io.IOException;
import java.nio.file.Path;

/** Supplies an operation-scoped NIO tree without constraining its {@link java.nio.file.FileSystem}. */
@FunctionalInterface
public interface SnapshotWorkspaceFactory {
  Workspace create() throws IOException;

  interface Workspace extends AutoCloseable {
    Path root();

    @Override
    void close() throws IOException;
  }
}
