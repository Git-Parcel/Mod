package io.github.leawind.gitparcel.server.commands;

import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.utils.permission.PermissionType;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelWorldSavedData;
import net.minecraft.commands.CommandSourceStack;

public abstract class GitParcelBaseCommand {

  protected static boolean validateWorldPermission(
      CommandSourceStack source, PermissionType<WorldPermissions> type) {
    return validateWorldPermission(source, type, GitParcelWorldSavedData.get(source.getServer()));
  }

  protected static boolean validateWorldPermission(
      CommandSourceStack source,
      PermissionType<WorldPermissions> type,
      GitParcelWorldSavedData worldSavedData) {
    PermissionConfig<WorldPermissions> permissions = worldSavedData.getPermissions();
    if (!permissions.permits(type, source.permissions())) {
      source.sendFailure(GitParcelTranslations.of("command.gitparcel.no_permission"));
      return false;
    }
    return true;
  }
}
