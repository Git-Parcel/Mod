package io.github.leawind.gitparcel.common.minecraft.logic.permission;

import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.common.api.permission.PermissionLevel;
import io.github.leawind.gitparcel.common.api.permission.PermissionType;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;

/**
 * Adapts Git Parcel's stable permission levels to Minecraft's permission-set API.
 *
 * <p>Keeping the conversion to {@link PermissionCheck} here prevents the version-specific
 * permission API from leaking into permission configs and command implementations.
 */
@VersionSensitive("Minecraft command permission API")
public final class MinecraftPermissions {
  private MinecraftPermissions() {}

  public static boolean hasPermission(
      CommandSourceStack source, PermissionLevel requiredLevel) {
    return checker(requiredLevel).check(source.permissions());
  }

  public static boolean hasPermission(ServerPlayer player, PermissionLevel requiredLevel) {
    return checker(requiredLevel).check(player.permissions());
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

  private static PermissionCheck checker(PermissionLevel level) {
    return switch (level) {
      case ALL -> Commands.LEVEL_ALL;
      case MODERATORS -> Commands.LEVEL_MODERATORS;
      case GAMEMASTERS -> Commands.LEVEL_GAMEMASTERS;
      case ADMINS -> Commands.LEVEL_ADMINS;
      case OWNERS -> Commands.LEVEL_OWNERS;
    };
  }
}
