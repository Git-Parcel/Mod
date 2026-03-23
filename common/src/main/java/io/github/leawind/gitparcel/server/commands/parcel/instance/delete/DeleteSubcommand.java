package io.github.leawind.gitparcel.server.commands.parcel.instance.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.commands.synchronization.ParcelSuggestionProvider;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DeleteSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var inst =
        Commands.argument("parcel", ParcelArgument.instance())
            .suggests(ParcelSuggestionProvider.INSTANCE);

    return Commands.literal("delete").then(inst.executes(DeleteSubcommand::delete));
  }

  private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var inst = ParcelArgument.getInstance(ctx, "parcel");
    return delete(ctx.getSource(), inst);
  }

  private static int delete(CommandSourceStack source, Parcel inst) {
    var serverLevel = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(serverLevel);
    var deleted = savedData.deleteParcel(inst.uuid());

    // TODO message

    if (deleted == null) {
      return 0;
    }

    return 1;
  }
}
