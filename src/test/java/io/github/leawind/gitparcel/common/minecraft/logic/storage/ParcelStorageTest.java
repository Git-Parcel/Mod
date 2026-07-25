package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParcelStorageTest {
  @TempDir Path tempDir;

  @Test
  void failedReplacementPreservesPreviousSnapshot() throws Exception {
    Path target = tempDir.resolve("parcel");
    Files.createDirectories(target);
    Files.writeString(target.resolve("old.txt"), "previous");

    assertThrows(
        IOException.class,
        () ->
            ParcelStorage.replaceDirectory(
                target,
                staging -> {
                  Files.writeString(staging.resolve("new.txt"), "incomplete");
                  throw new IOException("simulated write failure");
                }));

    assertEquals("previous", Files.readString(target.resolve("old.txt")));
    assertFalse(Files.exists(target.resolve("new.txt")));
    try (var files = Files.list(tempDir)) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void successfulReplacementRemovesPreviousSnapshot() throws Exception {
    Path target = tempDir.resolve("parcel");
    Files.createDirectories(target);
    Files.writeString(target.resolve("old.txt"), "previous");

    ParcelStorage.replaceDirectory(
        target, staging -> Files.writeString(staging.resolve("new.txt"), "complete"));

    assertFalse(Files.exists(target.resolve("old.txt")));
    assertTrue(Files.isRegularFile(target.resolve("new.txt")));
    assertEquals("complete", Files.readString(target.resolve("new.txt")));
    try (var files = Files.list(tempDir)) {
      assertEquals(1, files.count());
    }
  }
}
