package io.github.leawind.gitparcel.server.minecraft.logic.commands;

import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.PermissionType;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.utils.Translations;
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
    if (!MinecraftPermissions.permits(source, permissions, type)) {
      source.sendFailure(Translations.of("command.gitparcel.no_permission"));
      return false;
    }
    return true;
  }

  protected static boolean validateParcelPermission(
      CommandSourceStack source,
      Parcel parcel,
      PermissionType<ParcelPermissions> type) {
    if (!MinecraftPermissions.permits(source, parcel.permissions(), type)) {
      source.sendFailure(Translations.of("command.gitparcel.no_permission"));
      return false;
    }
    return true;
  }
}
