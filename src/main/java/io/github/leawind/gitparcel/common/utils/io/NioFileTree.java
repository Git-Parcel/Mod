package io.github.leawind.gitparcel.common.utils.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Provider-neutral, depth-bounded-memory operations on temporary NIO file trees. */
public final class NioFileTree {
  private NioFileTree() {}

  public static void deleteRecursivelyIfExists(Path root) throws IOException {
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path directory, IOException failure)
              throws IOException {
            if (failure != null) {
              throw failure;
            }
            Files.delete(directory);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /** Deletes every child while preserving the supplied directory itself. */
  public static void clearDirectory(Path directory) throws IOException {
    if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Expected a directory: " + directory);
    }
    try (var children = Files.newDirectoryStream(directory)) {
      for (Path child : children) {
        deleteRecursivelyIfExists(child);
      }
    }
  }

  /** Copies a regular-file tree into an existing or new destination directory. */
  public static void copyRecursively(Path source, Path destination) throws IOException {
    if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Expected a directory: " + source);
    }
    Files.createDirectories(destination);
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(
              Path directory, BasicFileAttributes attributes) throws IOException {
            if (!attributes.isDirectory()) {
              throw new IOException("Unsupported tree entry: " + directory);
            }
            Files.createDirectories(destination.resolve(source.relativize(directory).toString()));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (!attributes.isRegularFile()) {
              throw new IOException("Unsupported tree entry: " + file);
            }
            Files.copy(
                file,
                destination.resolve(source.relativize(file).toString()),
                StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
