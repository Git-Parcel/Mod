package io.github.leawind.gitparcel.utils.permissioni;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.utils.permission.PermissionTypeRegistry;
import java.util.Map;
import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PermissionConfigTest {

  private PermissionTypeRegistry<String> registry;
  private PermissionConfig<String> config;

  // Test permission types
  private PermissionType<String> ADMIN;
  private PermissionType<String> EDIT;
  private PermissionType<String> VIEW;
  private PermissionType<String> CUSTOM;

  @BeforeEach
  void setUp() {
    registry = new PermissionTypeRegistry<>();
    config = new PermissionConfig<>(registry);

    // Register test permission types with different default levels
    ADMIN = registry.register(new PermissionType<>((byte) 0, "admin", PermissionLevel.ADMINS));
    EDIT = registry.register(new PermissionType<>((byte) 1, "edit", PermissionLevel.MODERATORS));
    VIEW = registry.register(new PermissionType<>((byte) 2, "view", PermissionLevel.ALL));
    CUSTOM =
        registry.register(new PermissionType<>((byte) 3, "custom", PermissionLevel.GAMEMASTERS));
  }

  @Test
  void testIsSpecified_Unspecified() {
    assertFalse(config.isSpecified(ADMIN));
    assertFalse(config.isSpecified(EDIT));
  }

  @Test
  void testIsSpecified_Specified() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertTrue(config.isSpecified(ADMIN));
    assertFalse(config.isSpecified(EDIT));
  }

  @Test
  void testGet_DefaultLevel() {
    assertEquals(PermissionLevel.ADMINS, config.get(ADMIN));
    assertEquals(PermissionLevel.MODERATORS, config.get(EDIT));
    assertEquals(PermissionLevel.ALL, config.get(VIEW));
  }

  @Test
  void testGet_CustomLevel() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, config.get(ADMIN));

    config.set(EDIT, 2); // PermissionLevel.GAMEMASTERS
    assertEquals(PermissionLevel.GAMEMASTERS, config.get(EDIT));
  }

  @Test
  void testSet_WithPermissionLevel() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, config.get(ADMIN));
    assertTrue(config.isSpecified(ADMIN));

    config.set(ADMIN, PermissionLevel.MODERATORS);
    assertEquals(PermissionLevel.MODERATORS, config.get(ADMIN));
  }

  @Test
  void testSet_WithLevelId() {
    config.set(ADMIN, 4); // OWNERS
    assertEquals(PermissionLevel.OWNERS, config.get(ADMIN));

    config.set(ADMIN, 0); // ALL
    assertEquals(PermissionLevel.ALL, config.get(ADMIN));
  }

  @Test
  void testClear() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertTrue(config.isSpecified(ADMIN));

    config.clear(ADMIN);
    assertFalse(config.isSpecified(ADMIN));
    assertEquals(PermissionLevel.ADMINS, config.get(ADMIN)); // Back to default
  }

  @Test
  void testClear_UnspecifiedPermission() {
    config.clear(ADMIN);
    assertFalse(config.isSpecified(ADMIN));
  }

  @Test
  void testPermits_WithPermissionLevel() {
    // Default level check
    assertTrue(config.permits(VIEW, PermissionLevel.ALL));
    assertTrue(config.permits(VIEW, PermissionLevel.MODERATORS));
    assertTrue(config.permits(VIEW, PermissionLevel.ADMINS));
    assertFalse(config.permits(ADMIN, PermissionLevel.ALL));
    assertTrue(config.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(config.permits(ADMIN, PermissionLevel.OWNERS));

    // Custom level check
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertFalse(config.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(config.permits(ADMIN, PermissionLevel.OWNERS));
  }

  @Test
  void testToMap() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    config.set(EDIT, PermissionLevel.GAMEMASTERS);
    config.set(VIEW, PermissionLevel.MODERATORS);

    var map = config.toMap();

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
    assertTrue(config.toMap().isEmpty());
  }

  @Test
  void testFrom() {
    // Use the actual PermissionLevel IDs instead of hardcoded values
    Map<String, Byte> inputMap =
        Map.of(
            "admin", (byte) PermissionLevel.OWNERS.id(),
            "edit", (byte) PermissionLevel.GAMEMASTERS.id(),
            "view", (byte) PermissionLevel.ALL.id());

    PermissionConfig<String> loadedConfig = PermissionConfig.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedConfig.get(EDIT));
    assertEquals(PermissionLevel.ALL, loadedConfig.get(VIEW));
    assertTrue(loadedConfig.isSpecified(ADMIN));
    assertTrue(loadedConfig.isSpecified(EDIT));
    assertTrue(loadedConfig.isSpecified(VIEW));
  }

  @Test
  void testFrom_WithInvalidNames() {
    Map<String, Byte> inputMap =
        Map.of(
            "admin", (byte) PermissionLevel.OWNERS.id(),
            "nonexistent", (byte) 2, // This should be ignored
            "edit", (byte) PermissionLevel.GAMEMASTERS.id());

    PermissionConfig<String> loadedConfig = PermissionConfig.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedConfig.get(EDIT));
    assertTrue(loadedConfig.isSpecified(ADMIN));
    assertTrue(loadedConfig.isSpecified(EDIT));
    assertFalse(loadedConfig.isSpecified(VIEW));
  }

  @Test
  void testFrom_EmptyMap() {
    PermissionConfig<String> loadedConfig = PermissionConfig.from(registry, Map.of());

    assertEquals(PermissionLevel.ADMINS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.MODERATORS, loadedConfig.get(EDIT));
    assertFalse(loadedConfig.isSpecified(ADMIN));
  }

  @Test
  void testGetMapCodec_RoundTrip() {
    var codec = PermissionConfig.getMapCodec(registry);

    // Create configs with specific permissions
    config.set(ADMIN, PermissionLevel.OWNERS);
    config.set(EDIT, PermissionLevel.GAMEMASTERS);
    config.set(VIEW, PermissionLevel.MODERATORS);

    // Encode to map using DataResult
    var encodedResult = codec.encodeStart(JsonOps.INSTANCE, config);
    var encodedMap = encodedResult.getOrThrow();

    // Decode back
    var decodedResult = codec.parse(JsonOps.INSTANCE, encodedMap);
    var loadedConfig = decodedResult.getOrThrow();

    // Verify the decoded configs match the original
    assertEquals(PermissionLevel.OWNERS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedConfig.get(EDIT));
    assertEquals(PermissionLevel.MODERATORS, loadedConfig.get(VIEW));
    assertTrue(loadedConfig.isSpecified(ADMIN));
    assertTrue(loadedConfig.isSpecified(EDIT));
    assertTrue(loadedConfig.isSpecified(VIEW));
  }

  @Test
  void testMultiplePermissionTypes() {
    // Set various permissions
    config.set(ADMIN, PermissionLevel.OWNERS);
    config.set(EDIT, PermissionLevel.GAMEMASTERS);
    config.set(VIEW, PermissionLevel.ALL);
    config.set(CUSTOM, PermissionLevel.MODERATORS);

    // Verify all are correctly set
    assertEquals(PermissionLevel.OWNERS, config.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, config.get(EDIT));
    assertEquals(PermissionLevel.ALL, config.get(VIEW));
    assertEquals(PermissionLevel.MODERATORS, config.get(CUSTOM));

    // Verify permits
    assertTrue(config.permits(ADMIN, PermissionLevel.OWNERS));
    assertFalse(config.permits(ADMIN, PermissionLevel.ADMINS));
    assertTrue(config.permits(VIEW, PermissionLevel.ALL));
    assertTrue(config.permits(CUSTOM, PermissionLevel.GAMEMASTERS));
  }

  @Test
  void testOverwritePermission() {
    config.set(ADMIN, PermissionLevel.OWNERS);
    assertEquals(PermissionLevel.OWNERS, config.get(ADMIN));

    config.set(ADMIN, PermissionLevel.ALL);
    assertEquals(PermissionLevel.ALL, config.get(ADMIN));

    config.set(ADMIN, PermissionLevel.MODERATORS);
    assertEquals(PermissionLevel.MODERATORS, config.get(ADMIN));
  }
}
