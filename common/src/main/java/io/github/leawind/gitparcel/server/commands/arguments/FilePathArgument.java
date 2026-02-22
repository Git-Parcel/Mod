package io.github.leawind.gitparcel.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FilePathArgument implements ArgumentType<Path> {
  private static final Collection<String> EXAMPLES =
      Arrays.asList(
          "/home/steve/temp",
          "C:\\Users\\Steve\\temp",
          "./temp",
          "temp",
          "\"including space\"",
          "'quoted'");
  public static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Component.translatable("argument.filepath.invalid"));

  public static FilePathArgument filePath() {
    return new FilePathArgument();
  }

  public static Path getPath(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, Path.class);
  }

  @Override
  public Path parse(StringReader reader) throws CommandSyntaxException {
    try {
      return Path.of(reader.readString());
    } catch (InvalidPathException e) {
      throw ERROR_INVALID.createWithContext(reader);
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      final CommandContext<S> context, final SuggestionsBuilder builder) {
    // TODO
    return Suggestions.empty();
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
