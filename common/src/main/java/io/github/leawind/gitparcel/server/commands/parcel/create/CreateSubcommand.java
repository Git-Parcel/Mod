package io.github.leawind.gitparcel.server.commands.parcel.create;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CreateSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var showWireframe =
        Commands.argument("show_wireframe", BoolArgumentType.bool())
            .executes(CreateSubcommand::create2);

    var to =
        Commands.argument("to", BlockPosArgument.blockPos())
            .executes(CreateSubcommand::create1)
            .then(showWireframe);

    var from = Commands.argument("from", BlockPosArgument.blockPos()).then(to);

    return Commands.literal("create").then(from);
  }

  private static int create1(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        true);
  }

  private static int create2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return create(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        BoolArgumentType.getBool(ctx, "show_wireframe"));
  }

  private static int create(
      CommandContext<CommandSourceStack> ctx, BlockPos from, BlockPos to, boolean showWireframe) {
    var source = ctx.getSource();
    var level = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(level);

    // Check permission
    if (!validateWorldPermission(source, WorldPermissions.CREATE_PARCEL)) {
      return 0;
    }

    try {
      BoundingBox boundingBox = BoundingBox.fromCorners(from, to);
      UUID uuid = UUID.randomUUID();
      Parcel parcel = Parcel.from(uuid, boundingBox, new Parcel.Visual(showWireframe));

      savedData.addNewParcel(parcel);

      source.sendSuccess(
          () ->
              GitParcelTranslations.of(
                  "command.gitparcel.parcel.create.success",
                  from.toShortString(),
                  to.toShortString()),
          false);

      GitParcelMod.LOGGER.info("Created new parcel: from={}, to={}, uuid={}", from, to, uuid);

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
