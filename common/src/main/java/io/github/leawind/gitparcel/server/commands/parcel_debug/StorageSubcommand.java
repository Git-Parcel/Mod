package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.leawind.gitparcel.server.storage.StorageUtils;
import io.github.leawind.gitparcel.server.storage.SystemStorageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StorageSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("storage")
        .then(Commands.literal("world").executes(StorageSubcommand::world))
        .then(Commands.literal("game").executes(StorageSubcommand::game))
        .then(Commands.literal("system").executes(StorageSubcommand::system));
  }

  private static int system(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();

    source.sendSystemMessage(
        Component.literal("Data: " + SystemStorageManager.getDataDir().toString()));
    source.sendSystemMessage(
        Component.literal("Cache: " + SystemStorageManager.getCacheDir().toString()));
    source.sendSystemMessage(
        Component.literal("Secrets: " + SystemStorageManager.getSecrets().getDirPath().toString()));

    return 1;
  }

  private static int game(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = StorageUtils.gameStorage(source.getServer());

    source.sendSystemMessage(Component.literal("Game root: " + storage.getRoot()));
    source.sendSystemMessage(Component.literal("Game config file: " + storage.getConfigFile()));
    Message.sendSystemMessage(source, () -> "Game cached dir: " + storage.getCachedDir());
    Message.sendSystemMessage(source, () -> "Game shared dir: " + storage.getSharedDir());

    return 1;
  }

  private static int world(CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var storage = StorageUtils.worldStorage(source.getServer());

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
