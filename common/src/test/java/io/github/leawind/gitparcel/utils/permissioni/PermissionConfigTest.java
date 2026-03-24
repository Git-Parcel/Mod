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

  private PermissionTypeRegistry<Void> registry;
  private PermissionConfig<Void> config;

  // Test permission types
  private PermissionType<Void> ADMIN;
  private PermissionType<Void> EDIT;
  private PermissionType<Void> VIEW;
  private PermissionType<Void> CUSTOM;

  @BeforeEach
  void setUp() {
    registry = new PermissionTypeRegistry<>();
    config = new PermissionConfig<>(registry);

    // Register test permission types with different default levels
    ADMIN = registry.register(new PermissionType<>("admin", PermissionLevel.ADMINS));
    EDIT = registry.register(new PermissionType<>("edit", PermissionLevel.MODERATORS));
    VIEW = registry.register(new PermissionType<>("view,", PermissionLevel.ALL));
    CUSTOM = registry.register(new PermissionType<>("custom,", PermissionLevel.GAMEMASTERS));
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
    assertTrue(map.containsKey(ADMIN.id()));
    assertTrue(map.containsKey(EDIT.id()));
    assertTrue(map.containsKey(VIEW.id()));
  }

  @Test
  void testToMap_Empty() {
    assertTrue(config.toMap().isEmpty());
  }

  @Test
  void testFrom() {
    // Use the actual PermissionLevel IDs instead of hardcoded values
    var inputMap =
        Map.of(
            ADMIN.id(), (byte) PermissionLevel.OWNERS.id(),
            EDIT.id(), (byte) PermissionLevel.GAMEMASTERS.id(),
            VIEW.id(), (byte) PermissionLevel.ALL.id());

    var loadedConfig = PermissionConfig.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedConfig.get(EDIT));
    assertEquals(PermissionLevel.ALL, loadedConfig.get(VIEW));
    assertTrue(loadedConfig.isSpecified(ADMIN));
    assertTrue(loadedConfig.isSpecified(EDIT));
    assertTrue(loadedConfig.isSpecified(VIEW));
  }

  @Test
  void testFrom_WithInvalidNames() {
    var inputMap =
        Map.of(
            ADMIN.id(),
            (byte) PermissionLevel.OWNERS.id(),
            "unexist",
            (byte) 2, // This should be ignored
            EDIT.id(),
            (byte) PermissionLevel.GAMEMASTERS.id());

    var loadedConfig = PermissionConfig.from(registry, inputMap);

    assertEquals(PermissionLevel.OWNERS, loadedConfig.get(ADMIN));
    assertEquals(PermissionLevel.GAMEMASTERS, loadedConfig.get(EDIT));
    assertTrue(loadedConfig.isSpecified(ADMIN));
    assertTrue(loadedConfig.isSpecified(EDIT));
    assertFalse(loadedConfig.isSpecified(VIEW));
  }

  @Test
  void testFrom_EmptyMap() {
    var loadedConfig = PermissionConfig.from(registry, Map.of());

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
    var encodedMap = codec.encodeStart(JsonOps.INSTANCE, config).getOrThrow();

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
