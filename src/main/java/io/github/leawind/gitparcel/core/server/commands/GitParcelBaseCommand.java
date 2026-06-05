package io.github.leawind.gitparcel.core.server.commands;

import io.github.leawind.gitparcel.core.GitParcelTranslations;
import io.github.leawind.gitparcel.core.permission.WorldPermissions;
import io.github.leawind.gitparcel.core.utils.permission.PermissionConfig;
import io.github.leawind.gitparcel.core.utils.permission.PermissionType;
import io.github.leawind.gitparcel.core.world.GitParcelWorldSavedData;
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
    PermissionConfig<WorldPermissions> permissions = worldSavedData.permissions();
    if (!permissions.permits(type, source.permissions())) {
      source.sendFailure(GitParcelTranslations.of("command.gitparcel.no_permission"));
      return false;
    }
    return true;
  }
}
