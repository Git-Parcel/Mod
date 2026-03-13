package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.leawind.gitparcel.GitParcelTranslations;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;

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
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.filepath.invalid_path"));

  public static FilePathArgument path() {
    return new FilePathArgument();
  }

  public static Path getPath(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, Path.class);
  }

  @Override
  public Path parse(StringReader reader) throws CommandSyntaxException {
    // exclude special chars
    var str = reader.readString();
    if (str.matches(".*[:*?\"<>|\n].*")) {
      throw ERROR_INVALID_PATH.createWithContext(reader);
    }

    try {
      return Path.of(str);
    } catch (InvalidPathException e) {
      throw ERROR_INVALID_PATH.createWithContext(reader);
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
