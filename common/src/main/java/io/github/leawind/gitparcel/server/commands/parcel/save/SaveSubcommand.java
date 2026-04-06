package io.github.leawind.gitparcel.server.commands.parcel.save;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.commands.synchronization.ParcelSuggestionProvider;
import io.github.leawind.gitparcel.world.Parcel;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SaveSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var ignore_entities =
        Commands.argument("ignore_entities", BoolArgumentType.bool())
            .executes(SaveSubcommand::saveWithIgnoreEntities);

    return Commands.literal("save")
        .then(
            Commands.argument("parcel", ParcelArgument.parcel())
                .suggests(ParcelSuggestionProvider.INSTANCE)
                .executes(SaveSubcommand::save)
                .then(ignore_entities));
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
    Parcel parcel = ParcelArgument.getParcel(ctx, "parcel");

    try {
      // TODO git
      parcel.save(ignoreEntities);
      ctx.getSource()
          .sendSuccess(
              () ->
                  GitParcelTranslations.of(
                      "command.gitparcel.parcel.save.success", parcel.uuid().toString()),
              true);
      return 1;

    } catch (IOException | ParcelException e) {
      ctx.getSource()
          .sendFailure(
              GitParcelTranslations.of(
                  "command.gitparcel.parcel.save.failure",
                  e.getClass().getSimpleName() + ": " + e.getMessage()));
      return 0;
    } catch (Exception e) {
      ctx.getSource()
          .sendFailure(
              GitParcelTranslations.of(
                  "command.gitparcel.parcel.unexpected_error", e.getMessage()));
      return 0;
    }
  }
}
