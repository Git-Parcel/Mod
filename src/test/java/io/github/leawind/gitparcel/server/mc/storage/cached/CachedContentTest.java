package io.github.leawind.gitparcel.server.mc.storage.cached;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.leawind.gitparcel.server.mc.storage.cached.CachedContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CachedContentTest {
  private FileSystem fs;
  private Path tempDir;
  private CachedContent cachedContent;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem();
    tempDir = fs.getPath("/cached");
    Files.createDirectories(tempDir);
    cachedContent = new CachedContent(tempDir);
  }

  @AfterEach
  void afterEach() throws IOException {
    fs.close();
  }

  @Test
  void testGetRoot() {
    assertEquals(tempDir, cachedContent.getRoot());
  }

  @Test
  void testGetSourceDirCreatesDirectory() throws IOException {
    var dir = cachedContent.getSourceDir("github");
    assertEquals(tempDir.resolve("github"), dir);
    assertTrue(Files.exists(dir));
    assertTrue(Files.isDirectory(dir));
  }

  @Test
  void testGetSourceDirIsIdempotent() throws IOException {
    var dir1 = cachedContent.getSourceDir("github");
    var dir2 = cachedContent.getSourceDir("github");
    assertEquals(dir1, dir2);
    assertTrue(Files.exists(dir1));
  }

  @Test
  void testGetSourceDirDifferentSourceIds() throws IOException {
    var dir1 = cachedContent.getSourceDir("github");
    var dir2 = cachedContent.getSourceDir("curseforge");

    assertEquals(tempDir.resolve("github"), dir1);
    assertEquals(tempDir.resolve("curseforge"), dir2);
    assertNotEquals(dir1, dir2);
    assertTrue(Files.exists(dir1));
    assertTrue(Files.exists(dir2));
  }
}
