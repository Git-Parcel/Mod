package io.github.leawind.gitparcel.common.minecraft.logic.permission;

import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.common.api.permission.PermissionLevel;
import io.github.leawind.gitparcel.common.api.permission.PermissionType;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
/*? if >=1.21.11 {*/
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionCheck;
/*?}*/

/**
 * Adapts Git Parcel's stable permission levels to the current Minecraft command permission API.
 *
 * <p>Minecraft 1.21.11 replaced integer command permission checks with permission sets. Keeping
 * that conversion here prevents the version-specific API from leaking into permission configs and
 * command implementations.
 */
@VersionSensitive("Minecraft command permission API")
public final class MinecraftPermissions {
  private MinecraftPermissions() {}

  public static boolean hasPermission(
      CommandSourceStack source, PermissionLevel requiredLevel) {
    /*? if >=1.21.11 {*/
    return checker(requiredLevel).check(source.permissions());
    /*?} else {*/
    /*return source.hasPermission(requiredLevel.id());
     *//*?}*/
  }

  public static boolean hasPermission(ServerPlayer player, PermissionLevel requiredLevel) {
    /*? if >=1.21.11 {*/
    return checker(requiredLevel).check(player.permissions());
    /*?} else {*/
    /*return player.hasPermissions(requiredLevel.id());
     *//*?}*/
  }

  public static Predicate<CommandSourceStack> require(PermissionLevel requiredLevel) {
    return source -> hasPermission(source, requiredLevel);
  }

  public static <T> boolean permits(
      CommandSourceStack source, PermissionConfig<T> config, PermissionType<T> type) {
    return hasPermission(source, config.get(type));
  }

  public static <T> boolean permits(
      ServerPlayer player, PermissionConfig<T> config, PermissionType<T> type) {
    return hasPermission(player, config.get(type));
  }

  /*? if >=1.21.11 {*/
  private static PermissionCheck checker(PermissionLevel level) {
    return switch (level) {
      case ALL -> Commands.LEVEL_ALL;
      case MODERATORS -> Commands.LEVEL_MODERATORS;
      case GAMEMASTERS -> Commands.LEVEL_GAMEMASTERS;
      case ADMINS -> Commands.LEVEL_ADMINS;
      case OWNERS -> Commands.LEVEL_OWNERS;
    };
  }
  /*?}*/
}
