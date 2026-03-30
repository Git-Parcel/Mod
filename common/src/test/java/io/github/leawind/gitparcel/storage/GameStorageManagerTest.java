package io.github.leawind.gitparcel.storage;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.gitparcel.storage.cached.CachedContent;
import io.github.leawind.gitparcel.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GameStorageManagerTest {
  @TempDir Path tempDir;

  private GameStorageManager gameStorageManager;

  @BeforeEach
  void setUp() {
    gameStorageManager = new GameStorageManager(tempDir);
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
