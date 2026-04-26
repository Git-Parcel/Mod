package io.github.leawind.gitparcel.server.commands.parcel.save;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SaveSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var ignore_entities =
        Commands.argument("ignore_entities", BoolArgumentType.bool())
            .executes(SaveSubcommand::saveWithIgnoreEntities);

    return Commands.literal("save").executes(SaveSubcommand::save).then(ignore_entities);
  }

  private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    return save(ctx, true);
  }

  private static int saveWithIgnoreEntities(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return save(ctx, BoolArgumentType.getBool(ctx, "ignore_entities"));
  }

  private static int save(CommandContext<CommandSourceStack> ctx, boolean ignoreEntities)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, "parcel");

    for (var parcel : parcels) {
      try {
        parcel.save(ignoreEntities);
        source.sendSystemMessage(
            GitParcelTranslations.of(
                "command.gitparcel.parcel.save.success", parcel.uuid().toString()));

      } catch (IOException | ParcelException e) {
        source.sendFailure(
            GitParcelTranslations.of(
                "command.gitparcel.parcel.save.failure",
                e.getClass().getSimpleName() + ": " + e.getMessage()));
        return 0;
      } catch (Exception e) {
        source.sendFailure(
            GitParcelTranslations.of("command.gitparcel.parcel.unexpected_error", e.getMessage()));
        return 0;
      }
    }
    return 1;
  }
}
