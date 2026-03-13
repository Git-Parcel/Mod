package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.commands.arguments.DirPathArgument;
import io.github.leawind.gitparcel.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.commands.synchronization.FilePathSuggestionProvider;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ParcelDebugSaveSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var save_rotation =
        Commands.argument("rotation", TemplateRotationArgument.templateRotation())
            .executes(ParcelDebugSaveSubcommand::save5);

    var save_mirror =
        Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
            .executes(ParcelDebugSaveSubcommand::save4)
            .then(save_rotation);

    var save_ignore_entities =
        Commands.argument("ignore_entities", BoolArgumentType.bool())
            .executes(ParcelDebugSaveSubcommand::save3)
            .then(save_mirror);

    var save_format =
        Commands.argument("format", ParcelFormatArgument.saver())
            .executes(ParcelDebugSaveSubcommand::save2)
            .then(save_ignore_entities);

    var save_path =
        Commands.argument("path", DirPathArgument.path())
            .suggests(FilePathSuggestionProvider.DIRS.as())
            .executes(ParcelDebugSaveSubcommand::save1)
            .then(save_format);

    var save_to = Commands.argument("to", BlockPosArgument.blockPos()).then(save_path);

    var save_from = Commands.argument("from", BlockPosArgument.blockPos()).then(save_to);

    return Commands.literal("save").then(save_from);
  }

  private static int save1(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatRegistry.INSTANCE.defaultSaver(),
        true,
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int save2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        true,
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int save3(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        BoolArgumentType.getBool(ctx, "ignore_entities"),
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int save4(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        BoolArgumentType.getBool(ctx, "ignore_entities"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        Rotation.NONE);
  }

  private static int save5(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(
        ctx.getSource(),
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        DirPathArgument.getPath(ctx, "path"),
        ParcelFormatArgument.getSaver(ctx, "format"),
        BoolArgumentType.getBool(ctx, "ignore_entities"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        TemplateRotationArgument.getRotation(ctx, "rotation"));
  }

  private static int save(
      CommandSourceStack source,
      BlockPos corner1,
      BlockPos corner2,
      Path parcelDir,
      ParcelFormat.Save<?> format,
      boolean ignoreEntities,
      Mirror mirror,
      Rotation rotation) {
    try {
      BoundingBox boundingBox = BoundingBox.fromCorners(corner1, corner2);
      ParcelTransform transform = new ParcelTransform(mirror, rotation, boundingBox);

      Vec3i sizeWorldSpace =
          new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
      Vec3i sizeParcelSpace = ParcelTransform.rotateSizeInverted(rotation, sizeWorldSpace);

      ParcelMeta meta = new ParcelMeta(format.info(), sizeParcelSpace);

      ParcelFormat.save(source.getLevel(), transform, meta, parcelDir, ignoreEntities);

      source.sendSuccess(
          () -> GitParcelTranslations.of("command.gitparcel.parcel_debug.save.success"),
          ignoreEntities);

      return 1;

    } catch (IOException | ParcelException e) {
      ParcelDebugCommand.LOGGER.error("Error while saving parcel", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.save.failure",
              e.getClass().getSimpleName() + ": " + e.getMessage()));
      return 0;
    } catch (Exception e) {
      ParcelDebugCommand.LOGGER.error("Unexpected error while saving parcel", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
