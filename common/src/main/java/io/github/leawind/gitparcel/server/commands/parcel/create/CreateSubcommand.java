package io.github.leawind.gitparcel.server.commands.parcel.create;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CreateSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var rotation =
        Commands.argument("rotation", TemplateRotationArgument.templateRotation())
            .executes(CreateSubcommand::createAtRotation);

    var mirror =
        Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
            .executes(CreateSubcommand::createAtMirror)
            .then(rotation);

    var to =
        Commands.argument("to", BlockPosArgument.blockPos())
            .executes(CreateSubcommand::createAtTo)
            .then(mirror);

    var from = Commands.argument("from", BlockPosArgument.blockPos()).then(to);

    return Commands.literal("create").then(from);
  }

  private static int createAtTo(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int createAtMirror(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        Rotation.NONE);
  }

  private static int createAtRotation(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        TemplateRotationArgument.getRotation(ctx, "rotation"));
  }

  private static int create(
      CommandContext<CommandSourceStack> ctx,
      BlockPos from,
      BlockPos to,
      Mirror mirror,
      Rotation rotation) {
    var source = ctx.getSource();
    var level = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(level);

    // Check permission
    if (!validateWorldPermission(source, WorldPermissions.CREATE_PARCEL)) {
      return 0;
    }

    try {
      BoundingBox boundingBox = BoundingBox.fromCorners(from, to);
      Parcel parcel = Parcel.create(boundingBox, mirror, rotation);

      savedData.addNewParcel(parcel);

      source.sendSuccess(
          () ->
              GitParcelTranslations.of(
                  "command.gitparcel.parcel.create.success",
                  from.toShortString(),
                  to.toShortString()),
          false);

      GitParcelMod.LOGGER.info(
          "Created new parcel: from={}, to={}, uuid={}", from, to, parcel.uuid());

      return 1;

    } catch (IllegalArgumentException e) {
      GitParcelMod.LOGGER.error("Failed to create parcel: {}", e.getMessage(), e);
      source.sendFailure(
          GitParcelTranslations.of("command.gitparcel.parcel.create.failure", e.getMessage()));
      return 0;
    } catch (Exception e) {
      GitParcelMod.LOGGER.error("Unexpected error while creating parcel", e);
      source.sendFailure(
          GitParcelTranslations.of("command.gitparcel.parcel.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
