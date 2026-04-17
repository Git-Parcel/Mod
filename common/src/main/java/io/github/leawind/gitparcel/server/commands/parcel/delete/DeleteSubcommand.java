package io.github.leawind.gitparcel.server.commands.parcel.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.GitParcelLevelSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DeleteSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var parcel = Commands.argument("parcel", ParcelArgument.parcels());

    return Commands.literal("delete").then(parcel.executes(DeleteSubcommand::delete));
  }

  private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var source = ctx.getSource();

    if (!validateWorldPermission(source, WorldPermissions.DELETE_PARCEL)) {
      return 0;
    }

    var serverLevel = source.getLevel();
    var levelSavedData = GitParcelLevelSavedData.get(serverLevel);

    var parcels = ParcelArgument.getParcels(ctx, "parcel");

    for (var parcel : parcels) {
      levelSavedData.deleteParcel(parcel.uuid());
    }

    source.sendSuccess(
        () -> GitParcelTranslations.of("command.gitparcel.parcel.delete.success", parcels.size()),
        true);

    return parcels.size();
  }
}
