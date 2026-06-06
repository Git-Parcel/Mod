package io.github.leawind.gitparcel.mc.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.core.GitParcelTranslations;
import io.github.leawind.gitparcel.core.world.Parcel;
import io.github.leawind.gitparcel.mc.client.GitParcelClient;
import io.github.leawind.gitparcel.mc.commands.arguments.parcelselector.ParcelSelector;
import io.github.leawind.gitparcel.mc.commands.arguments.parcelselector.ParcelSelectorParser;
import io.github.leawind.gitparcel.mc.world.GitParcelLevelSavedData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.NonNull;

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
      names = GitParcelClient.PARCELS.values().stream().map(parcel -> parcel.meta().name());
    } else if (context.getSource() instanceof CommandSourceStack stack) {
      names =
          GitParcelLevelSavedData.get(stack.getLevel()).parcels().values().stream()
              .map(parcel -> parcel.meta().name());
    } else {
      names = Stream.empty();
    }

    if (source instanceof SharedSuggestionProvider provider) {
      names = names.filter(Objects::nonNull);
      StringReader reader = new StringReader(builder.getInput());
      reader.setCursor(builder.getStart());

      var parser = new ParcelSelectorParser(reader);

      try {
        parser.parse();
      } catch (CommandSyntaxException ignored) {
      }

      var frequency = names.collect(Collectors.groupingBy(n -> n, Collectors.counting()));

      final var finalNames =
          frequency.entrySet().stream()
              .filter(entry -> entry.getValue() == 1)
              .map(Map.Entry::getKey);

      return parser.fillSuggestions(
          builder, builder1 -> SharedSuggestionProvider.suggest(finalNames, builder1));
    } else {
      return Suggestions.empty();
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  public static class Info implements ArgumentTypeInfo<ParcelArgument, Info.Template> {
    private static final byte FLAG_SINGLE = 1;

    public void serializeToNetwork(Template template, @NonNull FriendlyByteBuf buf) {
      int i = 0;
      if (template.single) {
        i |= FLAG_SINGLE;
      }
      buf.writeByte(i);
    }

    public @NonNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
      byte b = buf.readByte();
      return new Template((b & FLAG_SINGLE) != 0);
    }

    public void serializeToJson(Template template, JsonObject obj) {
      obj.addProperty("isSingle", template.single);
    }

    public @NonNull Template unpack(ParcelArgument arg) {
      return new Template(arg.isSingle);
    }

    public final class Template implements ArgumentTypeInfo.Template<ParcelArgument> {
      final boolean single;

      Template(boolean single) {
        this.single = single;
      }

      public @NonNull ParcelArgument instantiate(@NonNull CommandBuildContext context) {
        return new ParcelArgument(this.single);
      }

      @Override
      public @NonNull ArgumentTypeInfo<ParcelArgument, ?> type() {
        return Info.this;
      }
    }
  }
}
