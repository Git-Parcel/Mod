package io.github.leawind.gitparcel.commands.synchronization;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.FilenameFilter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jspecify.annotations.Nullable;

public class FilePathSuggestionProvider<S> implements SuggestionProvider<S> {
  private static final Path DEFAULT_FROM = Path.of(".");

  public static final FilePathSuggestionProvider<?> ANY = any(DEFAULT_FROM);
  public static final FilePathSuggestionProvider<?> FILES = files(DEFAULT_FROM);
  public static final FilePathSuggestionProvider<?> DIRS = dirs(DEFAULT_FROM);

  public static <S> FilePathSuggestionProvider<S> any(Path from) {
    return new FilePathSuggestionProvider<>(from, (dir, name) -> true);
  }

  public static <S> FilePathSuggestionProvider<S> files(Path from) {
    return new FilePathSuggestionProvider<>(
        from, (dir, name) -> dir.toPath().resolve(name).toFile().isFile());
  }

  public static <S> FilePathSuggestionProvider<S> dirs(Path from) {
    return new FilePathSuggestionProvider<>(
        from, (dir, name) -> dir.toPath().resolve(name).toFile().isDirectory());
  }

  private final Path from;
  private final FilenameFilter filter;

  private FilePathSuggestionProvider(Path from, FilenameFilter filter) {
    this.from = from;
    this.filter = filter;
  }

  @SuppressWarnings("unchecked")
  public <T> FilePathSuggestionProvider<T> as() {
    return (FilePathSuggestionProvider<T>) this;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<S> context, SuggestionsBuilder builder) {
    do {
      try {
        String remaining = builder.getRemaining();

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
              parent = from;
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
          var entries = listDir(from);
          if (entries != null) {
            var suggestions =
                Arrays.stream(entries)
                    .filter(entry -> entry.startsWith(remaining))
                    .map(entry -> "'" + entry);
            return SharedSuggestionProvider.suggest(suggestions, builder);
          }
        }

      } catch (InvalidPathException | UnsupportedOperationException | SecurityException ignored) {
      }
    } while (false);

    return Suggestions.empty();
  }

  private @Nullable String[] listDir(Path dir) {
    return dir.toFile().list(filter);
  }
}
