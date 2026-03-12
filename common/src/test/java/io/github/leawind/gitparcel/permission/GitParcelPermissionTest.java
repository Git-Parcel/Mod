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
}
