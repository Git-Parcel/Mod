package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.server.commands.parcel.format.FormatSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.instance.InstanceSubcommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcel =
        Commands.literal("parcel")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .then(FormatSubcommand.build())
            .then(InstanceSubcommand.build());

    dispatcher.register(parcel);
  }
}
