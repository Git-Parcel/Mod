package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.server.commands.parcel.create.CreateSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.delete.DeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.formats.FormatsSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.list.ListSubcommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcel =
        Commands.literal("parcel")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .then(FormatsSubcommand.build())
            .then(CreateSubcommand.build())
            .then(DeleteSubcommand.build())
            .then(ListSubcommand.build());

    dispatcher.register(parcel);
  }
}
