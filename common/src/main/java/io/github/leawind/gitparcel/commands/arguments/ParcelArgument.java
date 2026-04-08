package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.commands.arguments.parcelselector.ParcelSelector;
import io.github.leawind.gitparcel.commands.arguments.parcelselector.ParcelSelectorParser;
import io.github.leawind.gitparcel.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;

/**
 * @see EntityArgument
 */
public class ParcelArgument implements ArgumentType<ParcelSelector> {
  private static final Collection<String> EXAMPLES =
      List.of(
          "\"My House\"",
          "#a",
          "#p",
          "#s",
          "#a[name=Base*,limit=3]",
          "dd12be42-52a9-4a91-a8a1-11c01849e498");

  public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PARCEL =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.too_many"));

  public static final SimpleCommandExceptionType ERROR_NO_PARCEL_FOUND =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.not_found"));

  public static final SimpleCommandExceptionType ERROR_SELECTOR_NOT_ALLOWED =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel.selector.not_allowed"));

  protected final boolean isSingle;

  protected ParcelArgument(boolean isSingle) {
    this.isSingle = isSingle;
  }

  public static ParcelArgument singleParcel() {
    return new ParcelArgument(true);
  }

  public static Parcel getSingleParcel(CommandContext<CommandSourceStack> context, String name)
      throws CommandSyntaxException {
    return context.getArgument(name, ParcelSelector.class).findSingleParcel(context.getSource());
  }

  public static ParcelArgument parcels() {
    return new ParcelArgument(false);
  }

  public static List<Parcel> getParcels(CommandContext<CommandSourceStack> context, String name)
      throws CommandSyntaxException {
    return context.getArgument(name, ParcelSelector.class).findParcels(context.getSource());
  }

  @Override
  public ParcelSelector parse(StringReader reader) throws CommandSyntaxException {
    return parse(reader, true);
  }

  private ParcelSelector parse(StringReader reader, boolean allowSelectors)
      throws CommandSyntaxException {
    var parser = new ParcelSelectorParser(reader);
    var selector = parser.parse();
    if (selector.getMaxResults() > 1 && isSingle) {
      reader.setCursor(0);
      throw ERROR_NOT_SINGLE_PARCEL.createWithContext(reader);
    } else {
      return selector;
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context, SuggestionsBuilder builder) {

    Stream<String> names;
    // TODO
    // source: ClientSuggestionProvider
    var source = context.getSource();
    if (context.getSource() instanceof ClientSuggestionProvider provider) {
      GitParcel.LOGGER.info("listSuggestions ClientSuggestionProvider");
      names = GitParcelClient.PARCELS.values().stream().map(parcel -> parcel.meta().name());
    } else if (context.getSource() instanceof CommandSourceStack stack) {
      GitParcel.LOGGER.info("listSuggestions CommandSourceStack");
      names =
          GitParcelLevelSavedData.get(stack.getLevel())
              .streamParcels()
              .map(parcel -> parcel.meta().name());
    } else {
      names = Stream.empty();
    }

    names = names.filter(Objects::nonNull);

    if (source instanceof SharedSuggestionProvider provider) {
      StringReader reader = new StringReader(builder.getInput());
      reader.setCursor(builder.getStart());

      var parser = new ParcelSelectorParser(reader);

      try {
        parser.parse();
      } catch (CommandSyntaxException ignored) {
      }

      final Stream<String> finalNames = names;
      return parser.fillSuggestions(
          builder,
          builder1 ->
              // TODO get unique parcel names
              SharedSuggestionProvider.suggest(finalNames, builder1));
    } else {
      return Suggestions.empty();
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
