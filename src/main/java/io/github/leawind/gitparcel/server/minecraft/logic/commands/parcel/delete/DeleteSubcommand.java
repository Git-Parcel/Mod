package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DeleteSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("delete").executes(DeleteSubcommand::delete);
  }

  private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var source = ctx.getSource();

    if (!validateWorldPermission(source, WorldPermissions.DELETE_PARCEL)) {
      return 0;
    }

    var serverLevel = source.getLevel();
    var parcelService = ParcelService.get(serverLevel);

    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    final int deleted;
    try {
      deleted = parcelService.deleteParcels(parcels);
    } catch (Exception e) {
      LOGGER.error("Failed to delete parcel registration", e);
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.delete.failure", describe(e)));
      return 0;
    }

    source.sendSuccess(
        () -> Translations.of("command.gitparcel.parcel.delete.success", deleted), true);

    return deleted;
  }
}
