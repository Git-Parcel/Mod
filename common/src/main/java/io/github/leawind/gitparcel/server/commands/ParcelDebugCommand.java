package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.GitParcelApi;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.server.commands.arguments.DirPathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;

public class ParcelDebugCommand {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    var save_ignore_entities =
        Commands.argument("ignore_entities", BoolArgumentType.bool())
            .executes(ParcelDebugCommand::save3);

    var save_format =
        Commands.argument("format", ParcelFormatArgument.saver())
            .executes(ParcelDebugCommand::save2)
            .then(save_ignore_entities);

    var save_path =
        Commands.argument("path", DirPathArgument.path())
            .executes(ParcelDebugCommand::save1)
            .then(save_format);

    var save_to = Commands.argument("to", BlockPosArgument.blockPos()).then(save_path);

    var save_from = Commands.argument("from", BlockPosArgument.blockPos()).then(save_to);

    var parcel_debug_save = Commands.literal("save").then(save_from);

    var load_rotation =
        Commands.argument("rotation", TemplateRotationArgument.templateRotation())
            .executes(ParcelDebugCommand::load3);

    var load_mirror =
        Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
            .executes(ParcelDebugCommand::load2)
            .then(load_rotation);

    var load_path =
        Commands.argument("path", DirPathArgument.path())
            .executes(ParcelDebugCommand::load1)
            .then(load_mirror);

    var load_from = Commands.argument("from", BlockPosArgument.blockPos()).then(load_path);

    var parcel_debug_load = Commands.literal("load").then(load_from);

    var parcel_debug =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(parcel_debug_save)
            .then(parcel_debug_load);

    dispatcher.register(parcel_debug);
  }

  private static int save1(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        GitParcelApi.FORMAT_REGISTRY.defaultSaver(),
        true);
  }

  private static int save2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        true);
  }

  private static int save3(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        BoolArgumentType.getBool(ctx, "ignore_entities"));
  }

  private static int save(
      CommandSourceStack source,
      BlockPos corner1,
      BlockPos corner2,
      Path parcelDir,
      ParcelFormat.Save<?> format,
      boolean ignoreEntities) {
    try {
      BoundingBox bounds = BoundingBox.fromCorners(corner1, corner2);
      Vec3i size = new Vec3i(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
      // Here transform rotation is none, so the real size is exactly the transformed size
      ParcelMeta meta = ParcelMeta.create(format.id(), format.version(), size);

      var transform =
          new ParcelTransform(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()));

      ParcelFormat.save(source.getLevel(), transform, meta, parcelDir, ignoreEntities);
      source.sendSuccess(
          () -> GitParcelTranslations.of("command.parcel_debug.save.success"), ignoreEntities);
      return 1;
    } catch (IOException | ParcelException e) {
      LOGGER.error("Error while saving parcel", e);
      source.sendFailure(GitParcelTranslations.of("command.parcel_debug.save.failure"));
      return 0;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while saving parcel", e);
      return 0;
    }
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
    return load(source, new ParcelTransform(mirror, rotation, pos), path, mirror, rotation);
  }

  private static int load(
      CommandSourceStack source,
      ParcelTransform transform,
      Path path,
      Mirror mirror,
      Rotation rotation) {
    final int loadFlags =
        Block.UPDATE_CLIENTS
            | Block.UPDATE_IMMEDIATE
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;

    try {
      // TODO load entities
      ParcelFormat.load(source.getLevel(), transform, path, true, false, loadFlags);

      source.sendSuccess(() -> GitParcelTranslations.of("command.parcel_debug.load.success"), true);
      return 1;
    } catch (IOException | ParcelException e) {
      LOGGER.error("Error while loading parcel", e);
      source.sendFailure(GitParcelTranslations.of("command.parcel_debug.load.failure"));
      return 0;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while loading parcel", e);
      return 0;
    }
  }
}
