package io.github.leawind.gitparcel.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.commands.synchronization.FilePathSuggestionProvider;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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

  public static final SimpleCommandExceptionType ERROR_CANNOT_BE_DIR =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.filepath.cannot_be_dir"));

  public static final SimpleCommandExceptionType ERROR_CANNOT_BE_FILE =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.filepath.cannot_be_file"));

  public static final SimpleCommandExceptionType ERROR_NOT_EXIST =
      new SimpleCommandExceptionType(
          GitParcelTranslations.of("argument.gitparcel.filepath.not_exist"));

  public static RequiredArgumentBuilder<CommandSourceStack, Path> argOfFile(
      String name, boolean allowUnexist) {
    var arg = new FilePathArgument(false, true, allowUnexist);
    return Commands.argument(name, arg).suggests(FilePathSuggestionProvider.FILES.as());
  }

  public static RequiredArgumentBuilder<CommandSourceStack, Path> argOfDir(
      String name, boolean allowUnexist) {
    var arg = new FilePathArgument(true, false, allowUnexist);
    return Commands.argument(name, arg).suggests(FilePathSuggestionProvider.DIRS.as());
  }

  public static Path getPath(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, Path.class);
  }

  private boolean flag;
  private final boolean allowDir;
  private final boolean allowFile;
  private final boolean allowUnexist;

  public FilePathArgument() {
    this(true, true, true);
    flag = true;
  }

  private FilePathArgument(boolean allowDir, boolean allowFile, boolean allowUnexist) {
    flag = false;

    this.allowDir = allowDir;
    this.allowFile = allowFile;
    this.allowUnexist = allowUnexist;
  }

  @Override
  public Path parse(StringReader reader) throws CommandSyntaxException {
    var str = reader.readString();

    // Check if contains special characters
    if (str.matches(".*[:*?\"<>|\n].*")) {
      throw ERROR_INVALID_PATH.createWithContext(reader);
    }

    Path path;
    try {
      path = Path.of(str);
    } catch (InvalidPathException e) {
      throw ERROR_INVALID_PATH.createWithContext(reader);
    }

    File file = path.toFile();

    if (!allowDir && file.isDirectory()) {
      throw ERROR_CANNOT_BE_DIR.createWithContext(reader);
    }

    if (!allowFile && file.isFile()) {
      throw ERROR_CANNOT_BE_FILE.createWithContext(reader);
    }

    if (!allowUnexist && !file.exists()) {
      throw ERROR_NOT_EXIST.createWithContext(reader);
    }

    return path;
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
