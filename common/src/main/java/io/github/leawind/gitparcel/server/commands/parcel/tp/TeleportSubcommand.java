package io.github.leawind.gitparcel.server.commands.parcel.tp;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.world.phys.Vec3;

/**
 * @see TeleportCommand
 */
public class TeleportSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("teleport")
        .executes(TeleportSubcommand::teleportSelf)
        .then(
            Commands.argument("players", EntityArgument.players())
                .executes(TeleportSubcommand::teleportPlayers));
  }

  private static int teleportSelf(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = ParcelArgument.getSingleParcel(ctx, "parcel");
    var player = source.getPlayerOrException();

    var pos = getTeleportPos(parcel);

    player.teleportTo(pos.x, pos.y, pos.z);

    source.sendSuccess(
        () ->
            Component.translatable(
                "commands.teleport.success.location.single",
                player.getDisplayName(),
                formatDouble(pos.x),
                formatDouble(pos.y),
                formatDouble(pos.z)),
        true);

    return 1;
  }

  private static int teleportPlayers(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = ParcelArgument.getSingleParcel(ctx, "parcel");
    var players = EntityArgument.getPlayers(ctx, "players");

    var pos = getTeleportPos(parcel);
    for (var player : players) {
      player.teleportTo(pos.x, pos.y, pos.z);
    }

    source.sendSuccess(
        () ->
            Component.translatable(
                "commands.teleport.success.location.multiple",
                players.size(),
                formatDouble(pos.x),
                formatDouble(pos.y),
                formatDouble(pos.z)),
        true);

    return players.size();
  }

  private static Vec3 getTeleportPos(Parcel parcel) {
    return parcel.getBoundingBox().getCenter().getBottomCenter();
  }

  private static String formatDouble(double value) {
    return String.format(Locale.ROOT, "%.2f", value);
  }
}
