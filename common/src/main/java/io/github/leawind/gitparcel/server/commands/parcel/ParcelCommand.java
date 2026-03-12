package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcel = Commands.literal("parcel").then(ParcelListSubcommand.build());

    dispatcher.register(parcel);
  }
}
