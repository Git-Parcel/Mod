package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.bind;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class UnbindSubcommand extends GitParcelBaseCommand {
  private UnbindSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("unbind").executes(UnbindSubcommand::unbind);
  }

  private static int unbind(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = BindSubcommand.singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.CONFIG)) {
      return 0;
    }

    ParcelService.get(source.getLevel()).unbindParcel(parcel);
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.parcel.unbind.success",
            parcel.uuid().toString()));
    return 1;
  }
}
