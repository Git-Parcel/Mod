package io.github.leawind.gitparcel.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.ParcelFormats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ParcelFormatArgument implements ArgumentType<ParcelFormat> {
  private static final Collection<String> EXAMPLES = Arrays.asList("mvp", "compressed_nbt");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Component.translatable("argument.parcel_format.invalid"));

  public static ParcelFormatArgument parcelFormat() {
    return new ParcelFormatArgument();
  }

  public static ParcelFormat getParcelFormat(
      CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ParcelFormat.class);
  }

  @Override
  public ParcelFormat parse(StringReader reader) throws CommandSyntaxException {
    String formatId = reader.readString();
    ParcelFormat format = ParcelFormats.of(formatId);
    if (format == null) {
      throw ERROR_INVALID.createWithContext(reader);
    }
    return format;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      final CommandContext<S> context, final SuggestionsBuilder builder) {
    return context.getSource() instanceof SharedSuggestionProvider
        ? SharedSuggestionProvider.suggest(ParcelFormats.getAllFormats().keySet().stream(), builder)
        : Suggestions.empty();
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
