package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.bind;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class BindSubcommand extends GitParcelBaseCommand {
  private static final String ARG_REPOSITORY = "repository";
  private static final String ARG_PATH = "path";

  private BindSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("bind")
        .then(
            Commands.argument(ARG_REPOSITORY, StringArgumentType.word())
                .suggests(BindSubcommand::suggestRepositories)
                .then(
                    Commands.argument(ARG_PATH, StringArgumentType.string())
                        .suggests(BindSubcommand::suggestParcelPaths)
                        .executes(BindSubcommand::bind)));
  }

  private static int bind(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      return 0;
    }
    Parcel parcel = singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.CONFIG)) {
      return 0;
    }

    String repository = StringArgumentType.getString(ctx, ARG_REPOSITORY);
    String path = StringArgumentType.getString(ctx, ARG_PATH);
    try {
      ParcelService.get(source.getLevel()).bindParcel(parcel, repository, path);
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.bind.success",
              parcel.uuid().toString(),
              repository,
              path));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Failed to bind parcel {} to {}/{}", parcel.uuid(), repository, path, e);
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.bind.failure", describe(e)));
      return 0;
    }
  }

  static Parcel singleParcel(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    if (parcels.isEmpty()) {
      throw ParcelArgument.ERROR_NO_PARCEL_FOUND.create();
    }
    if (parcels.size() != 1) {
      throw ParcelArgument.ERROR_NOT_SINGLE_PARCEL.create();
    }
    return parcels.getFirst();
  }

  public static CompletableFuture<Suggestions> suggestRepositories(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    try {
      return SharedSuggestionProvider.suggest(
          SharedRepositoryService.get(ctx.getSource().getServer()).list().keySet(),
          builder);
    } catch (IOException e) {
      return Suggestions.empty();
    }
  }

  public static CompletableFuture<Suggestions> suggestParcelPaths(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    try {
      String repository = StringArgumentType.getString(ctx, ARG_REPOSITORY);
      return SharedSuggestionProvider.suggest(
          SharedRepositoryService.get(ctx.getSource().getServer()).parcelPaths(repository),
          builder);
    } catch (Exception e) {
      return Suggestions.empty();
    }
  }

  public static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null ? "" : ": " + message);
  }
}
