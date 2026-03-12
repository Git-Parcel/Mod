package io.github.leawind.gitparcel.permission;

import java.util.regex.Pattern;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import org.jspecify.annotations.Nullable;

public class GitParcelPermission {
  public static final Pattern NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

  public static void validateName(@Nullable String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    // Check name
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "name must match pattern " + NAME_PATTERN.pattern() + ", got: " + name);
    }
  }

  public static PermissionLevel levelOf(PermissionSet set) {
    if (Commands.LEVEL_OWNERS.check(set)) {
      return PermissionLevel.OWNERS;
    } else if (Commands.LEVEL_ADMINS.check(set)) {
      return PermissionLevel.ADMINS;
    } else if (Commands.LEVEL_GAMEMASTERS.check(set)) {
      return PermissionLevel.GAMEMASTERS;
    } else if (Commands.LEVEL_MODERATORS.check(set)) {
      return PermissionLevel.MODERATORS;
    } else if (Commands.LEVEL_ALL.check(set)) {
      return PermissionLevel.ALL;
    } else {
      return PermissionLevel.ALL;
    }
  }

  public static PermissionCheck getChecker(byte permissionLevel) {
    return switch (permissionLevel) {
      case 0 -> Commands.LEVEL_ALL;
      case 1 -> Commands.LEVEL_MODERATORS;
      case 2 -> Commands.LEVEL_GAMEMASTERS;
      case 3 -> Commands.LEVEL_ADMINS;
      default -> Commands.LEVEL_OWNERS;
    };
  }
}
