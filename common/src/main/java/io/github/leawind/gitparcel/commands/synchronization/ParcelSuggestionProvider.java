package io.github.leawind.gitparcel.commands.synchronization;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.GitParcelWorldSavedData;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

@Deprecated
public class ParcelSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

  public static final ParcelSuggestionProvider INSTANCE = new ParcelSuggestionProvider();

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {

    var source = context.getSource();

    {
      // Check permission
      var server = source.getServer();
      var permissions = GitParcelWorldSavedData.get(server).getPermissions();
      if (!permissions.permits(WorldPermissions.LIST_PARCELS, source.permissions())) {
        return Suggestions.empty();
      }
    }

    var serverLevel = source.getLevel();
    var savedData = GitParcelLevelSavedData.get(serverLevel);
    var parcels = savedData.streamParcels();

    var suggestions =
        parcels
            .flatMap(parcel -> Stream.of(parcel.uuid().toString()))
            .filter(s -> s.startsWith(builder.getRemaining()));

    return SharedSuggestionProvider.suggest(suggestions, builder);
  }
}
