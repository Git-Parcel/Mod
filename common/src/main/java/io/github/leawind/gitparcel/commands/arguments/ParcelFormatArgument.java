package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

public final class ParcelFormatArgument {
  private static final Collection<String> EXAMPLES = Arrays.asList("mvp", "structure_template");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.parcel_format.invalid"));

  public static Saver saver() {
    return new Saver();
  }

  public static Loader loader() {
    return new Loader();
  }

  public static ParcelFormat.Saver<?> getSaver(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Saver.class);
  }

  public static ParcelFormat.Loader<?> getLoader(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Loader.class);
  }

  public static class Saver implements ArgumentType<ParcelFormat.Saver<?>> {

    @Override
    public ParcelFormat.Saver<?> parse(StringReader reader) throws CommandSyntaxException {
      var format = ParcelFormatRegistry.INSTANCE.getSaver(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(ParcelFormatRegistry.INSTANCE.getSaverNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }

  public static class Loader implements ArgumentType<ParcelFormat.Loader<?>> {

    @Override
    public ParcelFormat.Loader<?> parse(StringReader reader) throws CommandSyntaxException {
      var format = ParcelFormatRegistry.INSTANCE.getLoader(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(
              ParcelFormatRegistry.INSTANCE.getLoaderNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }
}
