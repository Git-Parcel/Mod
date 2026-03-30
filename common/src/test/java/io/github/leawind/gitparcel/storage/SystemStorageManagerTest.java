package io.github.leawind.gitparcel.storage;

import static org.junit.jupiter.api.Assertions.*;

import dev.dirs.BaseDirectories;
import io.github.leawind.gitparcel.storage.cached.CachedContent;
import io.github.leawind.gitparcel.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SystemStorageManagerTest {
  @TempDir Path tempDir;

  @Test
  void testCreate() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    assertNotNull(systemStorage);
    assertNotNull(systemStorage.getConfigFile());
    assertNotNull(systemStorage.getSecretDir());
    assertNotNull(systemStorage.getDefaultSharedDir());
  }

  @Test
  void testGetConfigFile() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();
    assertNotNull(configFile);
    assertTrue(configFile.toString().contains(SystemStorageManager.DIR_NAME));
    assertTrue(configFile.toString().contains(SystemStorageManager.CONFIG_FILE_NAME));
  }

  @Test
  void testGetSecretDir() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var secretDir = systemStorage.getSecretDir();
    assertNotNull(secretDir);
    assertTrue(secretDir.toString().contains(SystemStorageManager.DIR_NAME));
    assertTrue(secretDir.toString().contains("secrets"));
  }

  @Test
  void testGetDefaultSharedDir() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var defaultSharedDir = systemStorage.getDefaultSharedDir();
    assertNotNull(defaultSharedDir);
    assertTrue(defaultSharedDir.toString().contains(SystemStorageManager.DIR_NAME));
    assertTrue(defaultSharedDir.toString().contains(SharedContent.SHARED_DIR_NAME));
  }

  @Test
  void testGetSharedDirWithDefaultPath() throws IOException {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();

    // Create config file with empty object to use defaults
    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "{}");

    var sharedDir = systemStorage.getSharedDir();
    assertNotNull(sharedDir);
    assertEquals(systemStorage.getDefaultSharedDir(), sharedDir);
  }

  @Test
  void testGetCachedDirWithDefaultPath() throws IOException {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();

    // Create config file with empty object to use defaults
    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "{}");

    var cachedDir = systemStorage.getCachedDir();
    assertNotNull(cachedDir);
    // Should use default cached dir when no custom path is set
    assertTrue(cachedDir.toString().contains(CachedContent.CACHED_DIR_NAME));
  }

  @Test
  void testGetConfigWithEmptyFile() throws IOException {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();

    // Create parent directories and empty config file
    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "{}");

    var config = systemStorage.getConfig();
    assertNotNull(config);
    // Config is loaded successfully with defaults
  }

  @Test
  void testGetSharedDirWithCustomPath() throws IOException {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();

    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "{\"sharedDir\": \"/custom/shared\"}");

    var sharedDir = systemStorage.getSharedDir();
    assertEquals(Path.of("/custom/shared"), sharedDir);
  }

  @Test
  void testGetCachedDirWithCustomPath() throws IOException {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var configFile = systemStorage.getConfigFile();

    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, "{\"cachedDir\": \"/custom/cached\"}");

    var cachedDir = systemStorage.getCachedDir();
    assertEquals(Path.of("/custom/cached"), cachedDir);
  }

  @Test
  void testGetSecretManager() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var secretManager = systemStorage.getSecretManager();
    assertNotNull(secretManager);
  }

  @Test
  void testSecretManagerCaching() {
    var systemStorage = SystemStorageManager.create(BaseDirectories.get());
    var secretManager1 = systemStorage.getSecretManager();
    var secretManager2 = systemStorage.getSecretManager();

    // Should return the same cached instance
    assertSame(secretManager1, secretManager2);
  }
}
