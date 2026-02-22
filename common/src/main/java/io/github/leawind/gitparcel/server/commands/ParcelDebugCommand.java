package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
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
                                Commands.argument("path", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            saveParcel(
                                                ctx.getSource(),
                                                BoundingBox.fromCorners(
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                    BlockPosArgument.getLoadedBlockPos(ctx, "to")),
                                                StringArgumentType.getString(ctx, "path"))))));
    final var commandLoad =
        Commands.literal("load")
            .then(
                Commands.argument("from", BlockPosArgument.blockPos())
                    .then(
                        Commands.argument("path", StringArgumentType.string())
                            .executes(
                                ctx ->
                                    loadParcel(
                                        ctx.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                        StringArgumentType.getString(ctx, "path")))));
    final var command =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(commandSave)
            .then(commandLoad);

    dispatcher.register(command);
  }

  public static int saveParcel(CommandSourceStack source, BoundingBox bounds, String path) {
    try {
      var parcel = new Parcel(source.getLevel(), bounds);
      ParcelFormat.Registry.MVP_V0.save(parcel, Path.of(path));
      Constants.LOG.info("Saving parcel {} to {}", bounds, path);
      return 0;
    } catch (Exception e) {
      Constants.LOG.error("Error while saving parcel {} to {}", bounds, path, e);
      return 1;
    }
  }

  public static int loadParcel(CommandSourceStack source, BlockPos origin, String path) {
    // TODO
    Constants.LOG.info("Loading parcel {} from {}", origin, path);
    return 0;
  }
}
