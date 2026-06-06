package io.github.leawind.gitparcel.mc.server.commands;

import io.github.leawind.gitparcel.core.GitParcelTranslations;
import io.github.leawind.gitparcel.core.permission.PermissionConfig;
import io.github.leawind.gitparcel.core.permission.PermissionType;
import io.github.leawind.gitparcel.core.permission.WorldPermissions;
import io.github.leawind.gitparcel.mc.world.GitParcelWorldSavedData;
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
