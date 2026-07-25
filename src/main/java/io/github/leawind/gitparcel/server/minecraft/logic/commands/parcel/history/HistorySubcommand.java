package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.history;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class HistorySubcommand extends GitParcelBaseCommand {
  private static final int DEFAULT_LIMIT = 10;
  private static final int MAX_LIMIT = 50;

  private HistorySubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("history")
        .executes(ctx -> history(ctx, DEFAULT_LIMIT))
        .then(
            Commands.argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                .executes(
                    ctx ->
                        history(
                            ctx,
                            IntegerArgumentType.getInteger(ctx, "limit"))));
  }

  private static int history(CommandContext<CommandSourceStack> ctx, int limit)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.LOAD)) {
        return 0;
      }
    }

    var service = ParcelService.get(source.getLevel());
    for (var parcel : parcels) {
      try {
        var history = service.getParcelHistory(parcel, limit);
        if (history.isEmpty()) {
          source.sendSystemMessage(
              Translations.of(
                  "command.gitparcel.parcel.history.empty",
                  parcel.uuid().toString()));
          continue;
        }

        source.sendSystemMessage(
            Translations.of(
                "command.gitparcel.parcel.history.header",
                parcel.uuid().toString(),
                history.size()));
        for (var commit : history) {
          source.sendSystemMessage(
              Translations.of(
                  "command.gitparcel.parcel.history.entry",
                  abbreviate(commit.revision()),
                  commit.committedAt().toString(),
                  commit.author(),
                  commit.message()));
        }
      } catch (IOException | ParcelException e) {
        LOGGER.error("Failed to read history for parcel {}", parcel.uuid(), e);
        source.sendFailure(
            Translations.of(
                "command.gitparcel.parcel.history.failure",
                parcel.uuid().toString(),
                describe(e)));
        return 0;
      } catch (Exception e) {
        LOGGER.error("Unexpected error while reading parcel history {}", parcel.uuid(), e);
        source.sendFailure(
            Translations.of("command.gitparcel.parcel.unexpected_error", describe(e)));
        return 0;
      }
    }
    return 1;
  }

  private static String abbreviate(String revision) {
    return revision.substring(0, Math.min(8, revision.length()));
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null ? "" : ": " + message);
  }
}
