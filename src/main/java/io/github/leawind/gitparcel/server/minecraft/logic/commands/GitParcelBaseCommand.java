package io.github.leawind.gitparcel.server.minecraft.logic.commands;

import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.PermissionType;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
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

  protected static GitRepo.CommitIdentity snapshotIdentity(CommandSourceStack source) {
    String name =
        source
            .getTextName()
            .replace('<', '_')
            .replace('>', '_')
            .replace('\n', ' ')
            .replace('\r', ' ');
    if (name.isBlank()) {
      name = "Minecraft Server";
    }
    var entity = source.getEntity();
    String email =
        entity == null ? "server@gitparcel.local" : entity.getUUID() + "@gitparcel.local";
    return new GitRepo.CommitIdentity(name, email);
  }

  /** Stable operation ownership for permission-filtered status queries. */
  protected static String operationOwner(CommandSourceStack source) {
    var entity = source.getEntity();
    return entity == null ? source.getTextName() : entity.getUUID().toString();
  }

  protected static String abbreviate(String objectId) {
    return objectId.substring(0, Math.min(8, objectId.length()));
  }

  protected static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null || message.isBlank() ? "" : ": " + message);
  }
}
