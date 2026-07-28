package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

final class ParcelContentFileSupport {
  static final int MAX_DIRECTORY_RECORDS = 100_000;
  static final long MAX_SECTION_FILE_BYTES = 16L * 1024 * 1024;
  static final long MAX_RECORD_FILE_BYTES = 64L * 1024 * 1024;
  static final String EMPTY_DIRECTORY_MARKER = ".empty";

  private ParcelContentFileSupport() {}

  static CompoundTag readTag(NbtFormat format, Path path)
      throws ParcelException.CorruptedParcelException {
    try {
      requireFileSize(path, MAX_RECORD_FILE_BYTES, "NBT record");
    } catch (IOException e) {
      throw new ParcelException.CorruptedParcelException(
          "Failed to inspect record size: " + path, e);
    }
    var result = format.read(path);
    if (result.isErr()) {
      throw new ParcelException.CorruptedParcelException(
          "Failed to read %s: %s".formatted(path, result.unwrapErr()));
    }
    return result.unwrap();
  }

  static void requireFileSize(Path path, long maximum, String type) throws IOException {
    if (Files.size(path) > maximum) {
      throw new IOException(type + " exceeds the safety size limit: " + path);
    }
  }

  static void readRecordDirectory(Path directory, String suffix, FileConsumer consumer)
      throws IOException, ParcelException {
    if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
      return;
    }
    if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
      throw new ParcelException.CorruptedParcelException(
          "Expected content directory: " + directory);
    }
    var matching = new ArrayList<Path>();
    boolean hasEmptyMarker = false;
    try (var paths = Files.list(directory)) {
      var iterator = paths.iterator();
      while (iterator.hasNext()) {
        Path path = iterator.next();
        if (path.getFileName().toString().equals(EMPTY_DIRECTORY_MARKER)
            && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            && Files.size(path) == 0) {
          hasEmptyMarker = true;
          continue;
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            || !path.getFileName().toString().endsWith(suffix)) {
          throw new ParcelException.CorruptedParcelException(
              "Unexpected content entry: " + path);
        }
        if (matching.size() >= MAX_DIRECTORY_RECORDS) {
          throw new ParcelException.CorruptedParcelException(
              "Too many records in directory: " + directory);
        }
        matching.add(path);
      }
    }
    if (hasEmptyMarker && !matching.isEmpty()) {
      throw new ParcelException.CorruptedParcelException(
          "Empty marker accompanies records in directory: " + directory);
    }
    matching.sort(Comparator.comparing(item -> item.getFileName().toString()));
    for (Path path : matching) {
      consumer.accept(path);
    }
  }

  static void validateOwnedFiles(Path directory, Set<Path> expectedFiles)
      throws IOException, ParcelException.CorruptedParcelException {
    Set<Path> normalized =
        expectedFiles.stream()
            .map(path -> path.normalize())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    Path[] unexpected = {null};
    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
              throws IOException {
            if (!attributes.isRegularFile() || !normalized.contains(file.normalize())) {
              unexpected[0] = file;
              return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
          }
        });
    if (unexpected[0] != null) {
      throw new ParcelException.CorruptedParcelException(
          "Unexpected content entry: " + unexpected[0]);
    }
  }

  static final class ManagedOutput {
    private final Path root;
    private final Set<Path> liveFiles = new HashSet<>();

    ManagedOutput(Path root) {
      this.root = root.normalize();
    }

    Path file(Path path) throws IOException {
      Path normalized = path.normalize();
      if (!normalized.startsWith(root) || normalized.equals(root)) {
        throw new IOException("Content path escapes its directory: " + path);
      }
      if (!liveFiles.add(normalized)) {
        throw new IOException("Content file written more than once: " + path);
      }
      Path parent = normalized.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      return normalized;
    }

    Path file(String name) throws IOException {
      return file(root.resolve(name));
    }

    void finish() throws IOException {
      Files.createDirectories(root);
      Files.walkFileTree(
          root,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException {
              if (!attributes.isRegularFile()) {
                throw new IOException("Unsupported content entry: " + file);
              }
              if (!liveFiles.contains(file.normalize())) {
                Files.delete(file);
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure)
                throws IOException {
              if (failure != null) throw failure;
              if (!directory.equals(root)) {
                try (var entries = Files.newDirectoryStream(directory)) {
                  if (!entries.iterator().hasNext()) {
                    Files.delete(directory);
                  }
                }
              }
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  @FunctionalInterface
  interface FileConsumer {
    void accept(Path path) throws IOException, ParcelException;
  }
}
