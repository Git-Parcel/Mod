package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

public class ParcelInstanceArgument
    implements ArgumentType<ParcelInstanceArgument.ParcelInstanceSelector> {
  private static final Collection<String> EXAMPLES =
      Arrays.asList("dd12be42-52a9-4a91-a8a1-11c01849e498", "adder", "My House");

  public static final SimpleCommandExceptionType ERROR_TOO_MANY =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel_instance.toomany"));
  public static final SimpleCommandExceptionType ERROR_INSTANCE_NOT_FOUND =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel_instance.notfound"));

  public static ParcelInstanceArgument instance() {
    return new ParcelInstanceArgument();
  }

  public static ParcelInstance getInstance(CommandContext<CommandSourceStack> context, String name)
      throws CommandSyntaxException {
    return context.getArgument(name, ParcelInstance.class);
  }

  @Override
  public ParcelInstanceSelector parse(StringReader reader) throws CommandSyntaxException {
    String input = reader.readString();
    UUID uuid = UUID.fromString(input);
    return new ParcelInstanceSelector(uuid);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context, SuggestionsBuilder builder) {
    if (context.getSource() instanceof CommandSourceStack source) {
      String remaining = builder.getRemaining();

      // Get the current level to access parcel instances
      var serverLevel = source.getLevel();
      var savedData = GitParcelLevelSavedData.get(serverLevel);
      var instances = savedData.streamParcelInstances();

      // Suggest UUIDs and names
      var suggestions =
          instances
              .flatMap(inst -> Stream.of(inst.uuid().toString()))
              .filter(s -> s.startsWith(remaining));

      return SharedSuggestionProvider.suggest(suggestions, builder);
    } else {
      return Suggestions.empty();
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  /** A simple wrapper to represent either UUID-based or name-based lookup */
  public static class ParcelInstanceSelector {
    private final UUID uuid;

    private ParcelInstanceSelector(UUID uuid) {
      this.uuid = uuid;
    }

    /**
     * Finds a {@link ParcelInstance} based on the provided {@link CommandSourceStack}.
     *
     * <p>If the instance is found, it is returned. Otherwise, an exception is thrown.
     *
     * @param source The command source stack providing the context for the lookup.
     * @return The found {@link ParcelInstance}.
     * @throws CommandSyntaxException If the instance is not found
     */
    public ParcelInstance get(CommandSourceStack source) throws CommandSyntaxException {
      var serverLevel = source.getLevel();
      var savedData = GitParcelLevelSavedData.get(serverLevel);
      var inst = savedData.getParcelInstance(uuid);

      if (inst == null) {
        throw ERROR_INSTANCE_NOT_FOUND.create();
      }

      return inst;
    }
  }
}
