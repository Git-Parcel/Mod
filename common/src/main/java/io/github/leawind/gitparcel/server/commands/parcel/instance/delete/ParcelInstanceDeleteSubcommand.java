package io.github.leawind.gitparcel.server.commands.parcel.instance.delete;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.commands.arguments.ParcelInstanceArgument;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelInstanceDeleteSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var inst = Commands.argument("parcel_instance", ParcelInstanceArgument.instance());
    return Commands.literal("delete").then(inst).executes(ParcelInstanceDeleteSubcommand::delete);
  }

  private static int delete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var inst = ParcelInstanceArgument.getInstance(ctx, "parcel_instance");
    return delete(ctx.getSource(), inst);
  }

  private static int delete(CommandSourceStack source, ParcelInstance inst) {
    var serverLevel = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(serverLevel);
    var deleted = savedData.deleteParcelInstance(inst.uuid());

    // TODO message

    if (deleted == null) {
      return 0;
    }

    return 1;
  }
}
