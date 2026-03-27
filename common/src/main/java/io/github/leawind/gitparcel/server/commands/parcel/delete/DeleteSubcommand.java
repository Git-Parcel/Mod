package io.github.leawind.gitparcel.server.commands.parcel.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.commands.synchronization.ParcelSuggestionProvider;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DeleteSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var parcel =
        Commands.argument("parcel", ParcelArgument.parcel())
            .suggests(ParcelSuggestionProvider.INSTANCE);

    return Commands.literal("delete").then(parcel.executes(DeleteSubcommand::delete));
  }

  private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return delete(ctx, ParcelArgument.getParcel(ctx, "parcel"));
  }

  private static int delete(CommandContext<CommandSourceStack> ctx, Parcel parcel) {
    var source = ctx.getSource();
    var serverLevel = source.getLevel();

    // Check permission
    if (!validateWorldPermission(source, WorldPermissions.DELETE_PARCEL)) {
      return 0;
    }

    var levelSavedData = GitParcelLevelSavedData.get(serverLevel);
    var deleted = levelSavedData.deleteParcel(parcel.uuid());

    // TODO message

    if (deleted == null) {
      return 0;
    }

    return 1;
  }
}
