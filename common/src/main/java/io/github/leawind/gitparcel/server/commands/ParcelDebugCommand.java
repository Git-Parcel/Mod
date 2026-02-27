package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.ParcelMeta;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.server.commands.arguments.DirPathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
                                Commands.argument("path", DirPathArgument.path())
                                    .executes(ParcelDebugCommand::saveDefaultFormat)
                                    .then(
                                        Commands.argument("format", ParcelFormatArgument.saver())
                                            .executes(ParcelDebugCommand::save)))));

    var commandLoad =
        Commands.literal("load")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("path", DirPathArgument.path())
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
        DirPathArgument.getPath(ctx, "path"),
        Constants.PARCEL_FORMATS.defaultSaver());
  }

  private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"));
  }

  public static int save(
      CommandSourceStack source,
      BlockPos corner1,
      BlockPos corner2,
      Path parcelDir,
      ParcelFormat.Save format) {
    try {
      var parcel = Parcel.fromCorners(corner1, corner2);
      LOGGER.info(
          "Saving parcel (pos={}, size={}) with format {} to {}",
          parcel.getOrigin(),
          parcel.getSize(),
          format.id(),
          parcelDir);

      var meta = ParcelMeta.create(format.id(), format.version(), parcel.getSize());
      ParcelFormat.save(source.getLevel(), parcel, meta, parcelDir, true);
      source.sendSuccess(() -> Component.translatable("command.parcel_debug.save.success"), true);
      return 0;
    } catch (IOException | ParcelException e) {
      LOGGER.error("Error while saving parcel", e);
      source.sendFailure(Component.translatable("command.parcel_debug.save.failure"));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while saving parcel", e);
      return 2;
    }
  }

  private static int load(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return load(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        DirPathArgument.getPath(ctx, "path"));
  }

  public static int load(CommandSourceStack source, BlockPos pos, Path path) {
    try {
      LOGGER.info("Loading parcel at {} from {}", pos, path);
      // TODO load entities
      ParcelFormat.load(source.getLevel(), pos, path, true, false);

      source.sendSuccess(() -> Component.translatable("command.parcel_debug.load.success"), true);
      return 0;
    } catch (IOException | ParcelException e) {
      LOGGER.error("Error while loading parcel", e);
      source.sendFailure(Component.translatable("command.parcel_debug.load.failure"));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while loading parcel", e);
      return 2;
    }
  }
}
