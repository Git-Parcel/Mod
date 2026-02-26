package io.github.leawind.gitparcel.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class FilePathArgument implements ArgumentType<Path> {
  private static final Collection<String> EXAMPLES =
      Arrays.asList(
          "dir/file.txt",
          "/home/steve/temp",
          "C:\\Users\\Steve\\temp",
          "./temp",
          "temp",
          "\"including space\"",
          "'quoted'");
  public static final SimpleCommandExceptionType ERROR_INVALID_PATH =
      new SimpleCommandExceptionType(Component.translatable("argument.filepath.invalid_path"));
  public static final SimpleCommandExceptionType ERROR_INVALID_CHAR =
      new SimpleCommandExceptionType(Component.translatable("argument.filepath.invalid_char"));

  public static FilePathArgument path() {
    return new FilePathArgument();
  }

  public static Path getPath(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, Path.class);
  }

  protected String[] listDir(Path dir) {
    return dir.toFile().list();
  }

  @Override
  public Path parse(StringReader reader) throws CommandSyntaxException {
    try {
      // exclude special chars
      var str = reader.readString();
      if (str.matches(".*[:*?\"<>|\n].*")) {
        throw ERROR_INVALID_CHAR.createWithContext(reader);
      }
      return Path.of(str);
    } catch (InvalidPathException e) {
      throw ERROR_INVALID_PATH.createWithContext(reader);
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      final CommandContext<S> context, final SuggestionsBuilder builder) {
    if (context.getSource() instanceof SharedSuggestionProvider) {
      try {
        do {
          String remaining = builder.getRemaining();

          final Path cwd = Path.of(".");
          char quote = remaining.isEmpty() ? '!' : remaining.charAt(0);
          if (quote == '"' || quote == '\'') {
            if (remaining.charAt(remaining.length() - 1) == quote
                && remaining.length() > 1
                && remaining.charAt(remaining.length() - 2) != '\\') {
              break;
            }

            String unquoted = remaining.substring(1);

            var path = Path.of(unquoted);
            boolean endsWithSeperator = remaining.endsWith("/") || remaining.endsWith("\\");

            if (endsWithSeperator) {
              var entries = listDir(path);
              if (entries == null) {
                break;
              }
              var suggestions = Arrays.stream(entries).map(entry -> remaining + entry);
              return SharedSuggestionProvider.suggest(suggestions, builder);
            } else {
              var parent = path.getParent();

              if (parent == null && !path.isAbsolute()) {
                parent = cwd;
              }
              if (parent != null) {
                var half = path.getFileName().toString();
                var beforeHalf = remaining.substring(0, remaining.length() - half.length());

                var entries = listDir(parent);
                if (entries != null) {
                  var suggestions =
                      Arrays.stream(entries)
                          .filter(entry -> entry.startsWith(half))
                          .map(entry -> beforeHalf + entry);
                  return SharedSuggestionProvider.suggest(suggestions, builder);
                }
              }
            }

          } else {
            var entries = listDir(cwd);
            if (entries != null) {
              var suggestions = Arrays.stream(entries).filter(entry -> entry.startsWith(remaining));
              return SharedSuggestionProvider.suggest(suggestions, builder);
            }
          }

        } while (false);
      } catch (InvalidPathException | UnsupportedOperationException | SecurityException ignored) {
      }
    }
    return Suggestions.empty();
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
