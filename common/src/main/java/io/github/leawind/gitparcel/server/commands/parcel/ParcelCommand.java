package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.commands.parcel.config.ConfigSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.delete.DeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.save.SaveSubcommand;
import io.github.leawind.gitparcel.world.Parcel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                    .then(SaveSubcommand.build()));

    dispatcher.register(parcel);
  }

  private static int showInfo(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();

    if (!validateWorldPermission(source, WorldPermissions.LIST_PARCELS)) {
      return 0;
    }

    var parcel = ParcelArgument.getSingleParcel(ctx, "parcel");

    source.sendSuccess(() -> formatParcelInfo(parcel), false);
    return 1;
  }

  private static Component formatParcelInfo(Parcel parcel) {
    var meta = parcel.meta();
    var transform = parcel.transform();
    var bb = parcel.getBoundingBox();

    var component = Component.empty();
    component
        .append(Component.literal("UUID: ").append(Component.literal(parcel.uuid().toString())))
        .append(Component.literal("\n"))
        .append(Component.literal("Name: ").append(Component.literal(String.valueOf(meta.name()))))
        .append(Component.literal("\n"))
        .append(Component.literal("BoundingBox: ").append(Component.literal(bb.toString())))
        .append(Component.literal("\n"))
        .append(Component.literal("Transform: ").append(Component.literal(transform.toString())))
        .append(Component.literal("\n"))
        .append(
            Component.literal("Format: ").append(Component.literal(meta.formatSpec().toString())))
        .append(Component.literal("\n"))
        .append(Component.literal("Size: ").append(Component.literal(meta.size().toString())));

    if (meta.description() != null) {
      component
          .append(Component.literal("\n"))
          .append(Component.literal("Description: ").append(Component.literal(meta.description())));
    }

    return component;
  }
}
