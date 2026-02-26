package io.github.leawind.gitparcel.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class DirPathArgument extends FilePathArgument {
  private static final Collection<String> EXAMPLES =
      Arrays.asList(
          "/home/steve/temp",
          "C:\\Users\\Steve\\temp",
          "./temp",
          "temp",
          "\"including space\"",
          "'quoted'");

  public static final SimpleCommandExceptionType ERROR_NOT_DIR =
      new SimpleCommandExceptionType(Component.translatable("argument.filepath.not_dir"));

  public static DirPathArgument path() {
    return new DirPathArgument();
  }

  public static Path getPath(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, Path.class);
  }

  @Override
  protected String[] listDir(Path dir) {
    var children = super.listDir(dir);
    if (children == null) {
      return null;
    }
    return Arrays.stream(children)
        .filter(child -> dir.resolve(child).toFile().isDirectory())
        .toArray(String[]::new);
  }

  @Override
  public Path parse(StringReader reader) throws CommandSyntaxException {
    var path = super.parse(reader);
    if (path.toFile().isFile()) {
      throw ERROR_NOT_DIR.createWithContext(reader);
    }
    return path;
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
