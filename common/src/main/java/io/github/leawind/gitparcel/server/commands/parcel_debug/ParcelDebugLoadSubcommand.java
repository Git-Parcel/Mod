package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.commands.arguments.DirPathArgument;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public class ParcelDebugLoadSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var load_rotation =
        Commands.argument("rotation", TemplateRotationArgument.templateRotation())
            .executes(ParcelDebugLoadSubcommand::load3);

    var load_mirror =
        Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
            .executes(ParcelDebugLoadSubcommand::load2)
            .then(load_rotation);

    var load_path =
        Commands.argument("path", DirPathArgument.path())
            .executes(ParcelDebugLoadSubcommand::load1)
            .then(load_mirror);

    var load_from = Commands.argument("from", BlockPosArgument.blockPos()).then(load_path);

    return Commands.literal("load").then(load_from);
  }

  private static int load1(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return load(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        DirPathArgument.getPath(ctx, "path"),
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int load2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return load(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        DirPathArgument.getPath(ctx, "path"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        Rotation.NONE);
  }

  private static int load3(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return load(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        DirPathArgument.getPath(ctx, "path"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        TemplateRotationArgument.getRotation(ctx, "rotation"));
  }

  private static int load(
      CommandSourceStack source, BlockPos pos, Path path, Mirror mirror, Rotation rotation) {
    return load(source, new ParcelTransform(mirror, rotation, pos), path);
  }

  private static int load(CommandSourceStack source, ParcelTransform transform, Path path) {
    final int loadFlags =
        Block.UPDATE_CLIENTS
            | Block.UPDATE_IMMEDIATE
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;

    try {
      // TODO load entities
      ParcelFormat.load(source.getLevel(), transform, path, false, true, loadFlags);

      source.sendSuccess(
          () -> GitParcelTranslations.of("command.gitparcel.parcel_debug.load.success"), true);
      return 1;
    } catch (IOException | ParcelException e) {
      ParcelDebugCommand.LOGGER.error("Error while loading parcel", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.load.failure",
              e.getClass().getSimpleName() + ": " + e.getMessage()));
      return 0;
    } catch (Exception e) {
      ParcelDebugCommand.LOGGER.error("Unexpected error while loading parcel", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
