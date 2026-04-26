package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.commands.ParcelFormatter;
import io.github.leawind.gitparcel.server.commands.parcel.config.ConfigSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.delete.DeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.save.SaveSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.tp.TeleportSubcommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelCommand extends GitParcelBaseCommand {

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcel =
        Commands.literal("parcel")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .then(
                Commands.argument("parcel", ParcelArgument.singleParcel())
                    .executes(ParcelCommand::showInfo)
                    .then(ConfigSubcommand.build())
                    .then(DeleteSubcommand.build())
                    .then(SaveSubcommand.build())
                    .then(TeleportSubcommand.build()));

    dispatcher.register(parcel);
  }

  private static int showInfo(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();

    if (!validateWorldPermission(source, WorldPermissions.LIST_PARCELS)) {
      return 0;
    }

    var parcel = ParcelArgument.getSingleParcel(ctx, "parcel");

    source.sendSuccess(() -> ParcelFormatter.formatParcelInfo(parcel), false);
    return 1;
  }
}
