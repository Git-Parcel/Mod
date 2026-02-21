package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.Constants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;

public class ParcelDebugCommand {

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var commandSave =
        Commands.literal("save")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("to", BlockPosArgument.blockPos())
                            .then(
                                Commands.argument("path", ComponentArgument.textComponent(context))
                                    .executes(
                                        ctx -> {
                                          Constants.LOG.warn("parcel_debug save executing!");
                                          return 0;
                                        }))));

    final var commandLoad =
        Commands.literal("load")
            .then(
                Commands.argument("path", ComponentArgument.textComponent(context))
                    .executes(
                        ctx -> {
                          Constants.LOG.warn("parcel_debug load executing!");
                          return 0;
                        }));

    final var command =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(commandSave)
            .then(commandLoad);

    dispatcher.register(command);
  }
}
