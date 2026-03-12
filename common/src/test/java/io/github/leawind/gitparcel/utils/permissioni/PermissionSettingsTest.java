package io.github.leawind.gitparcel.utils.permissioni;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.utils.permission.PermissionSettings;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import java.util.Map;
import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PermissionSettingsTest {

  private PermissionTypeRegistry<String> registry;
  private PermissionSettings<String> settings;

  // Test permission types
  private PermissionType<String> ADMIN;
  private PermissionType<String> EDIT;
  private PermissionType<String> VIEW;
  private PermissionType<String> CUSTOM;

  @BeforeEach
  void setUp() {
    registry = new PermissionTypeRegistry<>();
    settings = new PermissionSettings<>(registry);

    // Register test permission types with different default levels
    ADMIN = registry.register(new PermissionType<>((byte) 0, "admin", PermissionLevel.ADMINS));
    EDIT = registry.register(new PermissionType<>((byte) 1, "edit", PermissionLevel.MODERATORS));
    VIEW = registry.register(new PermissionType<>((byte) 2, "view", PermissionLevel.ALL));
    CUSTOM =
        registry.register(new PermissionType<>((byte) 3, "custom", PermissionLevel.GAMEMASTERS));
  }

  @Test
  void testIsSpecified_Unspecified() {
    assertFalse(settings.isSpecified(ADMIN));
    assertFalse(settings.isSpecified(EDIT));
  }

  @Test
  void testIsSpecified_Specified() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertTrue(settings.isSpecified(ADMIN));
    assertFalse(settings.isSpecified(EDIT));
  }

  @Test
  void testGet_DefaultLevel() {
    assertEquals(PermissionLevel.ADMINS, settings.get(ADMIN));
    assertEquals(PermissionLevel.MODERATORS, settings.get(EDIT));
    assertEquals(PermissionLevel.ALL, settings.get(VIEW));
  }

  @Test
  void testGet_CustomLevel() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, settings.get(ADMIN));

    settings.set(EDIT, 2); // PermissionLevel.GAMEMASTERS
    assertEquals(PermissionLevel.GAMEMASTERS, settings.get(EDIT));
  }

  @Test
  void testSet_WithPermissionLevel() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, settings.get(ADMIN));
    assertTrue(settings.isSpecified(ADMIN));

    settings.set(ADMIN, PermissionLevel.MODERATORS);
    assertEquals(PermissionLevel.MODERATORS, settings.get(ADMIN));
  }

  @Test
  void testSet_WithLevelId() {
    settings.set(ADMIN, 4); // OWNERS
    assertEquals(PermissionLevel.OWNERS, settings.get(ADMIN));

    settings.set(ADMIN, 0); // ALL
    assertEquals(PermissionLevel.ALL, settings.get(ADMIN));
  }

  @Test
  void testClear() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertTrue(settings.isSpecified(ADMIN));

    settings.clear(ADMIN);
    assertFalse(settings.isSpecified(ADMIN));
    assertEquals(PermissionLevel.ADMINS, settings.get(ADMIN)); // Back to default
  }

  @Test
  void testClear_UnspecifiedPermission() {
    settings.clear(ADMIN);
    assertFalse(settings.isSpecified(ADMIN));
  }

  @Test
  void testPermits_WithPermissionLevel() {
    // Default level check
    assertTrue(settings.permits(VIEW, PermissionLevel.ALL));
    assertTrue(settings.permits(VIEW, PermissionLevel.MODERATORS));
    assertTrue(settings.permits(VIEW, PermissionLevel.ADMINS));
    assertFalse(settings.permits(ADMIN, PermissionLevel.ALL));
    assertTrue(settings.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(settings.permits(ADMIN, PermissionLevel.OWNERS));

    // Custom level check
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertFalse(settings.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(settings.permits(ADMIN, PermissionLevel.OWNERS));
  }

  @Test
  void testToMap() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    settings.set(EDIT, PermissionLevel.GAMEMASTERS);
    settings.set(VIEW, PermissionLevel.MODERATORS);

    var map = settings.toMap();

    assertEquals(3, map.size());
    // Verify all three permissions are in the map
    assertTrue(map.containsKey("admin"));
    assertTrue(map.containsKey("edit"));
    assertTrue(map.containsKey("view"));

    // Verify the levels by converting back
    assertEquals(PermissionLevel.OWNERS.id(), map.getByte("admin"));
    assertEquals(PermissionLevel.GAMEMASTERS.id(), map.getByte("edit"));
    assertEquals(PermissionLevel.MODERATORS.id(), map.getByte("view"));
  }

  @Test
  void testToMap_Empty() {
    assertTrue(settings.toMap().isEmpty());
  }

  @Test
  void testFrom() {
    // Use the actual PermissionLevel IDs instead of hardcoded values
    Map<String, Byte> inputMap =
        Map.of(
            "admin", (byte) PermissionLevel.OWNERS.id(),
            "edit", (byte) PermissionLevel.GAMEMASTERS.id(),
            "view", (byte) PermissionLevel.ALL.id());

    PermissionSettings<String> loadedSettings = PermissionSettings.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedSettings.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedSettings.get(EDIT));
    assertEquals(PermissionLevel.ALL, loadedSettings.get(VIEW));
    assertTrue(loadedSettings.isSpecified(ADMIN));
    assertTrue(loadedSettings.isSpecified(EDIT));
    assertTrue(loadedSettings.isSpecified(VIEW));
  }

  @Test
  void testFrom_WithInvalidNames() {
    Map<String, Byte> inputMap =
        Map.of(
            "admin", (byte) PermissionLevel.OWNERS.id(),
            "nonexistent", (byte) 2, // This should be ignored
            "edit", (byte) PermissionLevel.GAMEMASTERS.id());

    PermissionSettings<String> loadedSettings = PermissionSettings.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedSettings.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedSettings.get(EDIT));
    assertTrue(loadedSettings.isSpecified(ADMIN));
    assertTrue(loadedSettings.isSpecified(EDIT));
    assertFalse(loadedSettings.isSpecified(VIEW));
  }

  @Test
  void testFrom_EmptyMap() {
    PermissionSettings<String> loadedSettings = PermissionSettings.from(registry, Map.of());

    assertEquals(PermissionLevel.ADMINS, loadedSettings.get(ADMIN));
    assertEquals(PermissionLevel.MODERATORS, loadedSettings.get(EDIT));
    assertFalse(loadedSettings.isSpecified(ADMIN));
  }

  @Test
  void testGetMapCodec_RoundTrip() {
    var codec = PermissionSettings.getMapCodec(registry);

    // Create settings with specific permissions
    settings.set(ADMIN, PermissionLevel.OWNERS);
    settings.set(EDIT, PermissionLevel.GAMEMASTERS);
    settings.set(VIEW, PermissionLevel.MODERATORS);

    // Encode to map using DataResult
    var encodedResult = codec.encodeStart(JsonOps.INSTANCE, settings);
    var encodedMap = encodedResult.getOrThrow();

    // Decode back
    var decodedResult = codec.parse(JsonOps.INSTANCE, encodedMap);
    var decodedSettings = decodedResult.getOrThrow();

    // Verify the decoded settings match the original
    assertEquals(PermissionLevel.OWNERS, decodedSettings.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, decodedSettings.get(EDIT));
    assertEquals(PermissionLevel.MODERATORS, decodedSettings.get(VIEW));
    assertTrue(decodedSettings.isSpecified(ADMIN));
    assertTrue(decodedSettings.isSpecified(EDIT));
    assertTrue(decodedSettings.isSpecified(VIEW));
  }

  @Test
  void testMultiplePermissionTypes() {
    // Set various permissions
    settings.set(ADMIN, PermissionLevel.OWNERS);
    settings.set(EDIT, PermissionLevel.GAMEMASTERS);
    settings.set(VIEW, PermissionLevel.ALL);
    settings.set(CUSTOM, PermissionLevel.MODERATORS);

    // Verify all are correctly set
    assertEquals(PermissionLevel.OWNERS, settings.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, settings.get(EDIT));
    assertEquals(PermissionLevel.ALL, settings.get(VIEW));
    assertEquals(PermissionLevel.MODERATORS, settings.get(CUSTOM));

    // Verify permits
    assertTrue(settings.permits(ADMIN, PermissionLevel.OWNERS));
    assertFalse(settings.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(settings.permits(VIEW, PermissionLevel.ALL));
    assertTrue(settings.permits(CUSTOM, PermissionLevel.GAMEMASTERS));
  }

  @Test
  void testOverwritePermission() {
    settings.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, settings.get(ADMIN));

    settings.set(ADMIN, PermissionLevel.ALL);
    assertEquals(PermissionLevel.ALL, settings.get(ADMIN));

    settings.set(ADMIN, PermissionLevel.MODERATORS);
    assertEquals(PermissionLevel.MODERATORS, settings.get(ADMIN));
  }
}
