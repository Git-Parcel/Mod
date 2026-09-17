package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.resize;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.SharedRepositoryArguments;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Adjusts the parcel's content extent to a new world box; pure registration, CONFIG-gated. */
public final class ResizeSubcommand extends GitParcelBaseCommand {
  private ResizeSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("resize")
        .then(
            Commands.argument("from", BlockPosArgument.blockPos())
                .then(
                    Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(ResizeSubcommand::resize)));
  }

  private static int resize(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = SharedRepositoryArguments.singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.CONFIG)) {
      return 0;
    }

    BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
    BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
    var newBox = BoundingBox.fromCorners(from, to);

    var registry = ParcelRegistry.get(source.getLevel());
    try {
      registry.resizeParcel(parcel, newBox);
    } catch (ParcelException.Busy e) {
      source.sendFailure(Translations.of("command.gitparcel.parcel.resize.busy", parcel.uuid()));
      return 0;
    } catch (IllegalArgumentException e) {
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.resize.failure", e.getMessage()));
      return 0;
    }

    source.sendSuccess(
        () ->
            Translations.of(
                "command.gitparcel.parcel.resize.success",
                parcel.uuid().toString(),
                describeExtent(parcel)),
        true);
    parcel
        .archiveSync()
        .filter(
            sync ->
                !sync.size().equals(parcel.meta().size())
                    || !sync.anchor().equals(parcel.meta().anchor()))
        .ifPresent(
            sync ->
                source.sendSystemMessage(
                    Translations.of("command.gitparcel.parcel.resize.out_of_sync")));
    return 1;
  }

  private static Component describeExtent(Parcel parcel) {
    var size = parcel.meta().size();
    var bounds = parcel.getBoundingBox();
    return Component.literal(
        "%dx%dx%d at [%d %d %d]..[%d %d %d]"
            .formatted(
                size.getX(),
                size.getY(),
                size.getZ(),
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.maxX(),
                bounds.maxY(),
                bounds.maxZ()));
  }
}
