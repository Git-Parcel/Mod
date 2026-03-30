package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.leawind.gitparcel.storage.StorageManager;
import io.github.leawind.gitparcel.storage.SystemStorageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StorageSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("storage")
        .executes(StorageSubcommand::execute)
        .then(Commands.literal("world").executes(StorageSubcommand::world))
        .then(Commands.literal("game").executes(StorageSubcommand::game))
        .then(Commands.literal("system").executes(StorageSubcommand::system));
  }

  private static int execute(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = StorageManager.getInstance(source.getServer());

    Message.sendSystemMessage(source, () -> "Resolved cache dir: " + storage.resolveCachedDir());
    Message.sendSystemMessage(source, () -> "Resolved shared dir: " + storage.resolveSharedDir());

    return 1;
  }

  private static int system(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = SystemStorageManager.getInstance();

    source.sendSystemMessage(Component.literal("System config file: " + storage.getConfigFile()));
    source.sendSystemMessage(Component.literal("System secret dir: " + storage.getSecretDir()));
    Message.sendSystemMessage(source, () -> "System shared dir: " + storage.getSharedDir());
    Message.sendSystemMessage(source, () -> "System cached dir: " + storage.getCachedDir());

    return 1;
  }

  private static int game(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = StorageManager.getInstance(source.getServer()).gameStorage();

    source.sendSystemMessage(Component.literal("Game root: " + storage.getRoot()));
    source.sendSystemMessage(Component.literal("Game config file: " + storage.getConfigFile()));
    Message.sendSystemMessage(source, () -> "Game cached dir: " + storage.getCachedDir());
    Message.sendSystemMessage(source, () -> "Game shared dir: " + storage.getSharedDir());

    return 1;
  }

  private static int world(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = StorageManager.getInstance(source.getServer()).worldStorage();

    source.sendSystemMessage(Component.literal("World root: " + storage.getRoot()));

    return 1;
  }

  interface Message {
    String get() throws Exception;

    static void sendSystemMessage(CommandSourceStack source, Message msg) {
      try {
        source.sendSystemMessage(Component.literal(msg.get()));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
