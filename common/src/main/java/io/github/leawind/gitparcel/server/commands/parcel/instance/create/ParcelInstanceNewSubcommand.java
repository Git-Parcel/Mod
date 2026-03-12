package io.github.leawind.gitparcel.server.commands.parcel.instance.create;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.permission.GitParcelPermission;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ParcelInstanceNewSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var showBoundingBox =
        Commands.argument("show_bounding_box", BoolArgumentType.bool())
            .executes(ParcelInstanceNewSubcommand::createInstance2);

    var name =
        Commands.argument("name", StringArgumentType.string())
            .executes(ParcelInstanceNewSubcommand::createInstance1)
            .then(showBoundingBox);

    var to = Commands.argument("to", BlockPosArgument.blockPos()).then(name);

    var from = Commands.argument("from", BlockPosArgument.blockPos()).then(to);

    return Commands.literal("create").then(from);
  }

  private static int createInstance1(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return createInstance(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        StringArgumentType.getString(ctx, "name"),
        true);
  }

  private static int createInstance2(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return createInstance(
        ctx,
        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
        BlockPosArgument.getLoadedBlockPos(ctx, "to"),
        StringArgumentType.getString(ctx, "name"),
        BoolArgumentType.getBool(ctx, "show_bounding_box"));
  }

  private static int createInstance(
      CommandContext<CommandSourceStack> ctx,
      BlockPos from,
      BlockPos to,
      String name,
      boolean showBoundingBox) {
    var source = ctx.getSource();
    var level = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(level);

    // Check permission
    {
      // NOW
      var requiredLevel =
          GitParcelWorldSavedData.get(source.getServer())
              .getPermissions()
              .get(WorldPermissions.CREATE_PARCEL_INSTANCE);

      var sourceLevel = GitParcelPermission.levelOf(source.permissions());

      if (!sourceLevel.isEqualOrHigherThan(requiredLevel)) {
        source.sendFailure(GitParcelTranslations.of("command.gitparcel.no_permission"));
        return 0;
      }
    }

    try {
      BoundingBox boundingBox = BoundingBox.fromCorners(from, to);
      UUID uuid = UUID.randomUUID();
      ParcelInstance instance = new ParcelInstance(uuid, boundingBox, showBoundingBox);

      savedData.addNewParcelInstance(instance);

      source.sendSuccess(
          () -> GitParcelTranslations.of("command.parcel.instance.create.success", name, from, to),
          false);

      GitParcelMod.LOGGER.info(
          "Created new parcel instance: name={}, from={}, to={}, uuid={}", name, from, to, uuid);

      return 1;

    } catch (IllegalArgumentException e) {
      GitParcelMod.LOGGER.error("Failed to create parcel instance: {}", e.getMessage(), e);
      source.sendFailure(
          GitParcelTranslations.of("command.parcel.instance.create.failure", e.getMessage()));
      return 0;
    } catch (Exception e) {
      GitParcelMod.LOGGER.error("Unexpected error while creating parcel instance", e);
      source.sendFailure(
          GitParcelTranslations.of("command.parcel.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
