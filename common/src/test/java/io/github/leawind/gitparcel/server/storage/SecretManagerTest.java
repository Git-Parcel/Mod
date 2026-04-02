package io.github.leawind.gitparcel.server.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SecretManagerTest {
  @TempDir Path tempDir;

  private SecretManager secretManager;

  @BeforeEach
  void setUp() {
    secretManager = new SecretManager(tempDir);
  }

  @Test
  void testGetRoot() {
    assertEquals(tempDir, secretManager.getRoot());
  }

  @Test
  void testGetNonExistentKey() {
    assertNull(secretManager.get("nonexistent"));
  }

  @Test
  void testGetWhenFileNotExists() {
    // File doesn't exist yet
    assertNull(secretManager.get("any_key"));
  }

  @Test
  void testPutCreatesFile() throws IOException {
    assertFalse(Files.exists(tempDir.resolve("secrets.properties")));

    secretManager.put("key1", "value1");

    assertTrue(Files.exists(tempDir.resolve("secrets.properties")));
  }

  @Test
  void testPutAndGet() throws IOException {
    secretManager.put("api_token", "secret123");

    assertEquals("secret123", secretManager.get("api_token"));
  }

  @Test
  void testPutOverwritesValue() throws IOException {
    secretManager.put("key", "value1");
    assertEquals("value1", secretManager.get("key"));

    secretManager.put("key", "value2");
    assertEquals("value2", secretManager.get("key"));
  }

  @Test
  void testPutMultipleKeys() throws IOException {
    secretManager.put("key1", "value1");
    secretManager.put("key2", "value2");
    secretManager.put("key3", "value3");

    assertEquals("value1", secretManager.get("key1"));
    assertEquals("value2", secretManager.get("key2"));
    assertEquals("value3", secretManager.get("key3"));
  }

  @Test
  void testRemoveExistingKey() throws IOException {
    secretManager.put("key1", "value1");
    secretManager.put("key2", "value2");

    secretManager.remove("key1");

    assertNull(secretManager.get("key1"));
    assertEquals("value2", secretManager.get("key2"));
  }

  @Test
  void testRemoveNonExistentKey() throws IOException {
    // Should not throw exception
    secretManager.remove("nonexistent");
  }

  @Test
  void testRemoveWhenFileNotExists() throws IOException {
    // File doesn't exist, should not throw exception
    secretManager.remove("any_key");
  }

  @Test
  void testKeySetEmpty() {
    var keys = secretManager.keySet();
    assertNotNull(keys);
    assertTrue(keys.isEmpty());
  }

  @Test
  void testKeySetWithMultipleKeys() throws IOException {
    secretManager.put("key1", "value1");
    secretManager.put("key2", "value2");
    secretManager.put("key3", "value3");

    var keys = secretManager.keySet();

    assertEquals(3, keys.size());
    assertTrue(keys.contains("key1"));
    assertTrue(keys.contains("key2"));
    assertTrue(keys.contains("key3"));
  }

  @Test
  void testKeySetAfterRemove() throws IOException {
    secretManager.put("key1", "value1");
    secretManager.put("key2", "value2");

    secretManager.remove("key1");

    var keys = secretManager.keySet();

    assertEquals(1, keys.size());
    assertTrue(keys.contains("key2"));
    assertFalse(keys.contains("key1"));
  }

  @Test
  void testPutWithSpecialCharacters() throws IOException {
    String key = "key.with.dots";
    String value = "value with spaces and = signs";

    secretManager.put(key, value);

    assertEquals(value, secretManager.get(key));
  }

  @Test
  void testPutWithEmptyValue() throws IOException {
    secretManager.put("key", "");

    assertEquals("", secretManager.get("key"));
  }

  @Test
  void testPutWithUnicodeValue() throws IOException {
    String value = "日本語テスト";
    secretManager.put("key", value);

    assertEquals(value, secretManager.get("key"));
  }

  @Test
  void testMultipleOperationsSequence() throws IOException {
    // Initial state
    assertTrue(secretManager.keySet().isEmpty());

    // Add secrets
    secretManager.put("token1", "abc123");
    secretManager.put("token2", "xyz789");
    assertEquals(2, secretManager.keySet().size());

    // Update a secret
    secretManager.put("token1", "updated");
    assertEquals("updated", secretManager.get("token1"));
    assertEquals(2, secretManager.keySet().size());

    // Remove a secret
    secretManager.remove("token2");
    assertEquals(1, secretManager.keySet().size());
    assertNull(secretManager.get("token2"));
    assertEquals("updated", secretManager.get("token1"));
  }
}
