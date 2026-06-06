package io.github.leawind.gitparcel.mc.server.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.gitparcel.mc.server.storage.cached.CachedContent;
import io.github.leawind.gitparcel.mc.server.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GameStorageUtilsTest {
  private FileSystem fs;
  Path tempDir;

  private GameStorageManager gameStorageManager;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem();
    tempDir = fs.getPath("/tmp");
    Files.createDirectories(tempDir);

    gameStorageManager = new GameStorageManager(tempDir);
  }

  @AfterEach
  void afterEach() throws IOException {
    fs.close();
  }

  @Test
  void testGetRoot() {
    assertEquals(tempDir, gameStorageManager.getRoot());
  }

  @Test
  void testGetSharedDir() {
    var sharedDir = gameStorageManager.getSharedDir();
    assertNotNull(sharedDir);
    assertEquals(tempDir.resolve(SharedContent.SHARED_DIR_NAME), sharedDir);
  }

  @Test
  void testGetCachedDir() {
    var cachedDir = gameStorageManager.getCachedDir();
    assertNotNull(cachedDir);
    assertEquals(tempDir.resolve(CachedContent.CACHED_DIR_NAME), cachedDir);
  }

  @Test
  void testGetConfigWithDefaultValues() throws IOException {
    // Create a config file with empty object (should use defaults)
    var configFile = tempDir.resolve(GameStorageManager.CONFIG_FILE_NAME);
    Files.writeString(configFile, "{}");

    var config = gameStorageManager.getConfig();
    assertNotNull(config);
    assertFalse(config.useSystemStorage());
  }
}
