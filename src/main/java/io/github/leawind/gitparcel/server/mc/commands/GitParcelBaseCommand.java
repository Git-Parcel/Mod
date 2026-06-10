package io.github.leawind.gitparcel.server.mc.commands;

import io.github.leawind.gitparcel.core.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.core.api.permission.PermissionType;
import io.github.leawind.gitparcel.core.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.core.mc.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.core.util.Translations;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GitParcelBaseCommand {
  protected static Logger LOGGER = LoggerFactory.getLogger(GitParcelBaseCommand.class);

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
      source.sendFailure(Translations.of("command.gitparcel.no_permission"));
      return false;
    }
    return true;
  }
}
