package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import java.nio.file.Path;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

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
                                Commands.argument("path", FilePathArgument.filePath())
                                    .executes(
                                        ctx ->
                                            saveParcel(
                                                ctx.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "to"),
                                                FilePathArgument.getPath(ctx, "path"),
                                                Constants.PARCEL_FORMATS.defaultSaver()))
                                    .then(
                                        Commands.argument("format", ParcelFormatArgument.saver())
                                            .executes(
                                                ctx ->
                                                    saveParcel(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getLoadedBlockPos(
                                                            ctx, "from"),
                                                        BlockPosArgument.getLoadedBlockPos(
                                                            ctx, "to"),
                                                        FilePathArgument.getPath(ctx, "path"),
                                                        ParcelFormatArgument.getSaver(
                                                            ctx, "format")))))));
    final var commandLoad =
        Commands.literal("load")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("path", FilePathArgument.filePath())
                            .executes(
                                ctx ->
                                    loadParcel(
                                        ctx.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                        FilePathArgument.getPath(ctx, "path")))));
    final var command =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(commandSave)
            .then(commandLoad);

    dispatcher.register(command);
  }

  public static int saveParcel(
      CommandSourceStack source, BlockPos from, Vec3i to, Path path, ParcelFormat.Save format) {
    try {
      // TODO
      format.save(source.getLevel(), from, to, path, true, true);
      Constants.LOG.info(
          "Saving parcel [{}, {}] with format {} to {}", from, to, format.id(), path);
      return 0;
    } catch (Exception e) {
      Constants.LOG.error("Error while saving parcel", e);
      return 1;
    }
  }

  public static int loadParcel(CommandSourceStack source, BlockPos origin, Path path) {
    // TODO
    Constants.LOG.info("Loading parcel {} from {}", origin, path);
    return 0;
  }
}
