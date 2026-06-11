package io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.utils.Translations;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

public final class ParcelFormatArgument {
  private static final Collection<String> EXAMPLES = Arrays.asList("mvp", "structure_template");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Translations.of("argument.gitparcel.parcel_format.invalid"));

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
      var format = ParcelFormatRegistry.get().getSaver(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(ParcelFormatRegistry.get().getSaverNames(), builder)
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
      var format = ParcelFormatRegistry.get().getLoader(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(ParcelFormatRegistry.get().getLoaderNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }
}
