package io.github.leawind.gitparcel.server.minecraft.logic.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

/** Parsing helpers for explicit shared-repository exchange commands. */
public final class SharedRepositoryArguments {
  private SharedRepositoryArguments() {}

  public static Parcel singleParcel(CommandContext<CommandSourceStack> ctx)
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
          SharedRepositoryService.get(ctx.getSource().getServer()).list().keySet(), builder);
    } catch (IOException e) {
      return Suggestions.empty();
    }
  }

  public static CompletableFuture<Suggestions> suggestParcelPaths(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    try {
      String repository = StringArgumentType.getString(ctx, "repository");
      return SharedSuggestionProvider.suggest(
          SharedRepositoryService.get(ctx.getSource().getServer()).parcelPaths(repository), builder);
    } catch (Exception e) {
      return Suggestions.empty();
    }
  }
}
