package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.create;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
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

    var name =
        Commands.argument("name", StringArgumentType.string())
            .executes(CreateSubcommand::createAtName)
            .then(mirror);

    var to = Commands.argument("to", BlockPosArgument.blockPos()).then(name);

    var from = Commands.argument("from", BlockPosArgument.blockPos()).then(to);

    return Commands.literal("create").then(from);
  }

  private static int createAtName(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        StringArgumentType.getString(ctx, "name"),
        Mirror.NONE,
        Rotation.NONE);
  }

  private static int createAtMirror(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        StringArgumentType.getString(ctx, "name"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        Rotation.NONE);
  }

  private static int createAtRotation(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        StringArgumentType.getString(ctx, "name"),
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        TemplateRotationArgument.getRotation(ctx, "rotation"));
  }

  private static int create(
      CommandContext<CommandSourceStack> ctx,
      BlockPos from,
      BlockPos to,
      String name,
      Mirror mirror,
      Rotation rotation) {
    var source = ctx.getSource();
    var level = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(level);

    if (!validateWorldPermission(source, WorldPermissions.CREATE_PARCEL)) {
      return 0;
    }

    try {
      BoundingBox boundingBox = BoundingBox.fromCorners(from, to);
      Parcel parcel = Parcel.create(boundingBox, mirror, rotation);
      parcel.meta().setName(name);

      savedData.addNewParcel(parcel);

      source.sendSuccess(
          () ->
              Translations.of(
                  "command.gitparcel.parcel.create.success",
                  from.toShortString(),
                  to.toShortString()),
          false);

      LOGGER.info("Created new parcel: from={}, to={}, uuid={}", from, to, parcel.uuid());

      return 1;

    } catch (IllegalArgumentException e) {
      LOGGER.error("Failed to create parcel: {}", e.getMessage(), e);
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.create.failure", e.getMessage()));
      return 0;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while creating parcel", e);
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
