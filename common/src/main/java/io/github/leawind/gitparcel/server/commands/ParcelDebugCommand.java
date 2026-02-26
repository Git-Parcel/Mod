package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.slf4j.Logger;

public class ParcelDebugCommand {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    var commandSave =
        Commands.literal("save")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("to", BlockPosArgument.blockPos())
                            .then(
                                Commands.argument("path", FilePathArgument.filePath())
                                    .executes(ParcelDebugCommand::saveDefaultFormat)
                                    .then(
                                        Commands.argument("format", ParcelFormatArgument.saver())
                                            .executes(ParcelDebugCommand::save)))));

    var commandLoad =
        Commands.literal("load")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("path", FilePathArgument.filePath())
                            .executes(ParcelDebugCommand::load)));
    var command =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(commandSave)
            .then(commandLoad);

    dispatcher.register(command);
  }

  private static int saveDefaultFormat(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        FilePathArgument.getPath(ctx, "path"),
        Constants.PARCEL_FORMATS.defaultSaver());
  }

  private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        FilePathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"));
  }

  public static int save(
      CommandSourceStack source, BlockPos from, Vec3i to, Path path, ParcelFormat.Save format) {
    try {
      LOGGER.info("Saving parcel [{}, {}] with format {} to {}", from, to, format.id(), path);
      var size =
          new Vec3i(
              to.getX() - from.getX() + 1,
              to.getY() - from.getY() + 1,
              to.getZ() - from.getZ() + 1);
      format.save(source.getLevel(), from, size, path, true, true);
      return 0;
    } catch (Exception e) {
      LOGGER.error("Error while saving parcel", e);
      return 1;
    }
  }

  private static int load(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return load(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        FilePathArgument.getPath(ctx, "path"));
  }

  public static int load(CommandSourceStack source, BlockPos pos, Path path) {
    try {
      LOGGER.info("Loading parcel at {} from {}", pos, path);
      // TODO load entities
      Parcel.load(source.getLevel(), pos, path, true, false);
      return 0;
    } catch (IOException | Parcel.ParcelException e) {
      LOGGER.error("Error while loading parcel", e);
      return 1;
    }
  }
}
