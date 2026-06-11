package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelLevelSavedData;
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
    var levelSavedData = GitParcelLevelSavedData.get(serverLevel);

    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      levelSavedData.deleteParcel(parcel.uuid());
    }

    source.sendSuccess(
        () -> Translations.of("command.gitparcel.parcel.delete.success", parcels.size()),
        true);

    return parcels.size();
  }
}
