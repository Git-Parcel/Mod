package io.github.leawind.gitparcel.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ParcelFormatArgument {
  private static final Collection<String> EXAMPLES = Arrays.asList("mvp", "structure_template");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Component.translatable("argument.parcel_format.invalid"));

  public static Saver saver() {
    return new Saver();
  }

  public static Loader loader() {
    return new Loader();
  }

  public static ParcelFormat.Save getSaver(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Save.class);
  }

  public static ParcelFormat.Load getLoader(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.Load.class);
  }

  public static class Saver implements ArgumentType<ParcelFormat.Save> {

    @Override
    public ParcelFormat.Save parse(StringReader reader) throws CommandSyntaxException {
      var format = Constants.PARCEL_FORMATS.getSaver(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(Constants.PARCEL_FORMATS.getSaverNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }

  public static class Loader implements ArgumentType<ParcelFormat.Load> {

    @Override
    public ParcelFormat.Load parse(StringReader reader) throws CommandSyntaxException {
      var format = Constants.PARCEL_FORMATS.getLoader(reader.readString());
      if (format == null) {
        throw ERROR_INVALID.createWithContext(reader);
      }
      return format;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(
        final CommandContext<S> context, final SuggestionsBuilder builder) {
      return context.getSource() instanceof SharedSuggestionProvider
          ? SharedSuggestionProvider.suggest(Constants.PARCEL_FORMATS.getLoaderNames(), builder)
          : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
      return EXAMPLES;
    }
  }
}
