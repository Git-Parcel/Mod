package io.github.leawind.gitparcel.permission;

import static io.github.leawind.gitparcel.permission.GitParcelPermission.COMMIT;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.DEL_INSTANCE;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.LIST_FORMAT;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.LIST_INSTANCE;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.LOAD_INSTANCE;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.MOD_INSTANCE;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.NEW_INSTANCE;
import static io.github.leawind.gitparcel.permission.GitParcelPermission.SAVE_INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class GitParcelPermissionTest {

  @Test
  void testDefaultPermissionLevels() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // Test that unspecified permissions return their default levels
    assertEquals(LIST_FORMAT.defaultLevel(), settings.get(LIST_FORMAT));
    assertEquals(LIST_INSTANCE.defaultLevel(), settings.get(LIST_INSTANCE));
    assertEquals(SAVE_INSTANCE.defaultLevel(), settings.get(SAVE_INSTANCE));
    assertEquals(LOAD_INSTANCE.defaultLevel(), settings.get(LOAD_INSTANCE));
    assertEquals(NEW_INSTANCE.defaultLevel(), settings.get(NEW_INSTANCE));
    assertEquals(MOD_INSTANCE.defaultLevel(), settings.get(MOD_INSTANCE));
    assertEquals(DEL_INSTANCE.defaultLevel(), settings.get(DEL_INSTANCE));
    assertEquals(COMMIT.defaultLevel(), settings.get(COMMIT));
  }

  @Test
  void testSetAndGetPermissions() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // Set custom permission levels
    settings.set(LIST_FORMAT, 0);
    settings.set(SAVE_INSTANCE, 4);
    settings.set(NEW_INSTANCE, 2);

    // Verify the permissions are set correctly
    assertEquals((byte) 0, settings.get(LIST_FORMAT));
    assertEquals((byte) 4, settings.get(SAVE_INSTANCE));
    assertEquals((byte) 2, settings.get(NEW_INSTANCE));

    // Verify other permissions still return default levels
    assertEquals(LIST_INSTANCE.defaultLevel(), settings.get(LIST_INSTANCE));
    assertEquals(LOAD_INSTANCE.defaultLevel(), settings.get(LOAD_INSTANCE));
  }

  @Test
  void testIsSpecified() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // Initially, no permissions should be specified
    assertFalse(settings.isSpecified(LIST_FORMAT));
    assertFalse(settings.isSpecified(SAVE_INSTANCE));

    // After setting, permissions should be specified
    settings.set(LIST_FORMAT, 0);
    assertTrue(settings.isSpecified(LIST_FORMAT));
    assertFalse(settings.isSpecified(SAVE_INSTANCE));

    settings.set(SAVE_INSTANCE, 4);
    assertTrue(settings.isSpecified(SAVE_INSTANCE));
  }

  @Test
  void testClearPermissions() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // Set and verify
    settings.set(LIST_FORMAT, 0);
    assertTrue(settings.isSpecified(LIST_FORMAT));
    assertEquals((byte) 0, settings.get(LIST_FORMAT));

    // Clear and verify
    settings.clear(LIST_FORMAT);
    assertFalse(settings.isSpecified(LIST_FORMAT));
    assertEquals(LIST_FORMAT.defaultLevel(), settings.get(LIST_FORMAT));
  }

  @Test
  void testPermits() {
    // Test permission level checks
    assertTrue(GitParcelPermission.permits(0, 4));
    assertTrue(GitParcelPermission.permits(3, 3));
    assertTrue(GitParcelPermission.permits(1, 2));
    assertFalse(GitParcelPermission.permits(4, 3));
    assertFalse(GitParcelPermission.permits(3, 0));
  }

  @Test
  void testToLong() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // No specified permissions should return 0 for level 0
    assertEquals(0L, settings.toLong(0));

    // Set permissions that should be granted at level 3
    settings.set(SAVE_INSTANCE, 3); // default is 3
    settings.set(LIST_FORMAT, 0); // default is 1

    // At level 3, SAVE_INSTANCE should be granted (3 >= 3)
    // LIST_FORMAT should also be granted (3 >= 0)
    long result = settings.toLong(3);
    assertTrue((result & SAVE_INSTANCE.mask()) != 0);
    assertTrue((result & LIST_FORMAT.mask()) != 0);

    // At level 2, SAVE_INSTANCE should NOT be granted (2 < 3)
    result = settings.toLong(2);
    assertFalse((result & SAVE_INSTANCE.mask()) != 0);
    assertTrue((result & LIST_FORMAT.mask()) != 0);
  }

  @Test
  void testToMap() {
    var settings = new PermissionSettings(GitParcelPermission.REGISTRY);

    // Empty settings should return empty map
    assertTrue(settings.toMap().isEmpty());

    // Set some permissions
    settings.set(LIST_FORMAT, (byte) 0);
    settings.set(SAVE_INSTANCE, (byte) 4);
    settings.set(NEW_INSTANCE, (byte) 2);

    var map = settings.toMap();
    assertEquals(3, map.size());
    assertEquals((byte) 0, map.get(LIST_FORMAT.name()));
    assertEquals((byte) 4, map.get(SAVE_INSTANCE.name()));
    assertEquals((byte) 2, map.get(NEW_INSTANCE.name()));
  }

  @Test
  void testSettingsFromMap() {
    var map =
        Map.of(
            LIST_FORMAT.name(), (byte) 0,
            SAVE_INSTANCE.name(), (byte) 4,
            NEW_INSTANCE.name(), (byte) 2);

    var settings = PermissionSettings.from(GitParcelPermission.REGISTRY, map);

    assertTrue(settings.isSpecified(LIST_FORMAT));
    assertTrue(settings.isSpecified(SAVE_INSTANCE));
    assertTrue(settings.isSpecified(NEW_INSTANCE));

    assertEquals((byte) 0, settings.get(LIST_FORMAT));
    assertEquals((byte) 4, settings.get(SAVE_INSTANCE));
    assertEquals((byte) 2, settings.get(NEW_INSTANCE));

    // Unspecified permissions should use defaults
    assertFalse(settings.isSpecified(LIST_INSTANCE));
    assertEquals(LIST_INSTANCE.defaultLevel(), settings.get(LIST_INSTANCE));
  }

  @Test
  void testSettingsFromMapWithUnknownPermission() {
    // Map with unknown permission type should not fail
    var map = Map.of(LIST_FORMAT.name(), (byte) 0, "unknown_permission", (byte) 4);

    var settings = PermissionSettings.from(GitParcelPermission.REGISTRY, map);

    assertTrue(settings.isSpecified(LIST_FORMAT));
    assertEquals((byte) 0, settings.get(LIST_FORMAT));

    // Unknown permission should be ignored
    assertFalse(settings.isSpecified(SAVE_INSTANCE));
    assertEquals(SAVE_INSTANCE.defaultLevel(), settings.get(SAVE_INSTANCE));
  }

  @Test
  void testRoundTripSerialization() {
    var originalSettings = new PermissionSettings(GitParcelPermission.REGISTRY);
    originalSettings.set(LIST_FORMAT, (byte) 0);
    originalSettings.set(SAVE_INSTANCE, (byte) 4);
    originalSettings.set(NEW_INSTANCE, (byte) 2);
    originalSettings.set(COMMIT, (byte) 1);

    // Convert to map
    var map = originalSettings.toMap();

    // Convert back to settings
    var restoredSettings = PermissionSettings.from(GitParcelPermission.REGISTRY, map);

    // Verify all permissions match
    assertEquals(originalSettings.get(LIST_FORMAT), restoredSettings.get(LIST_FORMAT));
    assertEquals(originalSettings.get(SAVE_INSTANCE), restoredSettings.get(SAVE_INSTANCE));
    assertEquals(originalSettings.get(NEW_INSTANCE), restoredSettings.get(NEW_INSTANCE));
    assertEquals(originalSettings.get(COMMIT), restoredSettings.get(COMMIT));

    // Verify unspecified permissions still use defaults
    assertEquals(LIST_INSTANCE.defaultLevel(), restoredSettings.get(LIST_INSTANCE));
    assertEquals(LOAD_INSTANCE.defaultLevel(), restoredSettings.get(LOAD_INSTANCE));
  }

  @Test
  void testPermissionTypeMask() {
    // Verify each permission has a unique bit mask
    long[] masks = {
      LIST_FORMAT.mask(),
      LIST_INSTANCE.mask(),
      SAVE_INSTANCE.mask(),
      LOAD_INSTANCE.mask(),
      NEW_INSTANCE.mask(),
      MOD_INSTANCE.mask(),
      DEL_INSTANCE.mask(),
      COMMIT.mask()
    };

    // Check that all masks are unique powers of 2
    for (long mask : masks) {
      assertTrue(mask > 0, "Mask should be positive");
      assertTrue((mask & (mask - 1)) == 0, "Mask should be power of 2: " + mask);
    }

    // Check uniqueness
    for (int i = 0; i < masks.length; i++) {
      for (int j = i + 1; j < masks.length; j++) {
        assertNotEquals(masks[i], masks[j], "Masks at " + i + " and " + j + " should differ");
      }
    }
  }

  @Test
  void testPermissionTypeValidation() {
    // Valid permission type
    var validType = new PermissionType((byte) 10, "valid_test_name", (byte) 2);
    assertNotNull(validType);
    assertEquals((byte) 10, validType.id());
    assertEquals("valid_test_name", validType.name());
    assertEquals((byte) 2, validType.defaultLevel());

    // Invalid id (negative)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) -1, "test", (byte) 1));

    // Invalid id (> 63)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) 64, "test", (byte) 1));

    // Invalid name (starts with number)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) 10, "1test", (byte) 1));

    // Invalid name (contains special chars)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) 10, "test!", (byte) 1));

    // Invalid defaultLevel (< 0)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) 10, "test", (byte) -1));

    // Invalid defaultLevel (> 4)
    assertThrows(
        IllegalArgumentException.class, () -> new PermissionType((byte) 10, "test", (byte) 5));
  }

  @Test
  void testRegistryDuplicateId() {
    var registry = new PermissionTypeRegistry();
    registry.register(0, "test_perm_1", 1);

    // Registering another permission with same ID should fail
    assertThrows(IllegalArgumentException.class, () -> registry.register(0, "test_perm_2", 1));
  }

  @Test
  void testRegistryDuplicateName() {
    var registry = new PermissionTypeRegistry();
    registry.register(0, "test_perm", 1);

    // Registering another permission with same name should fail
    assertThrows(IllegalArgumentException.class, () -> registry.register(1, "test_perm", 1));
  }

  @Test
  void testRegistryGetByIdAndName() {
    var registry = new PermissionTypeRegistry();
    var type1 = registry.register(0, "test_by_id", 1);
    var type2 = registry.register(1, "test_by_name", 2);

    // Test retrieval by ID
    assertEquals(type1, registry.byId(0));
    assertEquals(type2, registry.byId(1));
    assertNull(registry.byId(2));

    // Test retrieval by name
    assertEquals(type1, registry.byName("test_by_id"));
    assertEquals(type2, registry.byName("test_by_name"));
    assertNull(registry.byName("nonexistent"));
  }

  @Test
  void testRegistryMaxId() {
    var registry = new PermissionTypeRegistry();
    assertEquals((byte) -1, registry.getMaxId());

    registry.register(0, "perm_0", 1);
    assertEquals((byte) 0, registry.getMaxId());

    registry.register(5, "perm_5", 1);
    assertEquals((byte) 5, registry.getMaxId());

    registry.register(3, "perm_3", 1);
    assertEquals((byte) 5, registry.getMaxId()); // Max should still be 5
  }
}
