package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.Constants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
    final var command =
        Commands.literal("parcel")
            .executes(
                (ctx) -> {
                  Constants.LOG.info("parcel command executed!");
                  return 0;
                });
    dispatcher.register(command);
  }
}
