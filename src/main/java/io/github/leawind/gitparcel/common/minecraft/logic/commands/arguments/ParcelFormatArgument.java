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
  private static final Collection<String> EXAMPLES =
      Arrays.asList("parcella_d16", "parcella_d32");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Translations.of("argument.gitparcel.parcel_format.invalid"));

  public static Writer writer() {
    return new Writer();
  }

  public static Reader reader() {
    return new Reader();
  }

  public static ParcelFormat.Writer<?> getWriter(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Writer.class);
  }

  public static ParcelFormat.Reader<?> getReader(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Reader.class);
  }

  public static class Writer implements ArgumentType<ParcelFormat.Writer<?>> {

    @Override
    public ParcelFormat.Writer<?> parse(StringReader reader) throws CommandSyntaxException {
      var format = ParcelFormatRegistry.get().getWriter(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(ParcelFormatRegistry.get().getWriterNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }

  public static class Reader implements ArgumentType<ParcelFormat.Reader<?>> {

    @Override
    public ParcelFormat.Reader<?> parse(StringReader reader) throws CommandSyntaxException {
      var format = ParcelFormatRegistry.get().getReader(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(ParcelFormatRegistry.get().getReaderNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }
}
