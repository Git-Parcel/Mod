package io.github.leawind.gitparcel.common.impl.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.permission.PermissionLevel;
import org.junit.jupiter.api.Test;

class PermissionLevelTest {

  @Test
  void preservesVanillaOperatorLevelIds() {
    assertEquals(0, PermissionLevel.ALL.id());
    assertEquals(1, PermissionLevel.MODERATORS.id());
    assertEquals(2, PermissionLevel.GAMEMASTERS.id());
    assertEquals(3, PermissionLevel.ADMINS.id());
    assertEquals(4, PermissionLevel.OWNERS.id());
  }

  @Test
  void resolvesAndClampsPersistedIds() {
    assertEquals(PermissionLevel.ALL, PermissionLevel.byId(-1));
    assertEquals(PermissionLevel.GAMEMASTERS, PermissionLevel.byId(2));
    assertEquals(PermissionLevel.OWNERS, PermissionLevel.byId(5));
  }

  @Test
  void comparesGrantedAndRequiredLevels() {
    assertTrue(PermissionLevel.ADMINS.includes(PermissionLevel.MODERATORS));
    assertTrue(PermissionLevel.ADMINS.includes(PermissionLevel.ADMINS));
    assertFalse(PermissionLevel.ADMINS.includes(PermissionLevel.OWNERS));
  }
}
