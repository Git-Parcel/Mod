package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.ParcelFormats;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.nio.file.Path;

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
                                                BoundingBox.fromCorners(
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "to")),
                                                FilePathArgument.getPath(ctx, "path"),
                                                ParcelFormats.MVP_V0))
                                    .then(
                                        Commands.argument(
                                                "format", ParcelFormatArgument.parcelFormat())
                                            .executes(
                                                ctx ->
                                                    saveParcel(
                                                        ctx.getSource(),
                                                        BoundingBox.fromCorners(
                                                            BlockPosArgument.getLoadedBlockPos(
                                                                ctx, "from"),
                                                            BlockPosArgument.getLoadedBlockPos(
                                                                ctx, "to")),
                                                        FilePathArgument.getPath(ctx, "path"),
                                                        ParcelFormatArgument.getParcelFormat(
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
      CommandSourceStack source, BoundingBox bounds, Path path, ParcelFormat format) {
    try {
      var parcel = new Parcel(source.getLevel(), bounds);
      format.save(parcel, path);
      Constants.LOG.info("Saving parcel {} to {} with format {}", bounds, path, format.id);
      return 0;
    } catch (Exception e) {
      Constants.LOG.error(
          "Error while saving parcel {} to {} with format {}", bounds, path, format.id, e);
      return 1;
    }
  }

  public static int loadParcel(CommandSourceStack source, BlockPos origin, Path path) {
    // TODO
    Constants.LOG.info("Loading parcel {} from {}", origin, path);
    return 0;
  }
}
