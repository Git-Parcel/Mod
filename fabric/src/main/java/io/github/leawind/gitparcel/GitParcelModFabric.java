package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class GitParcelModFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();
    GitParcelModFabric.init();
  }

  public static void init() {
    registerEvents();
  }

  private static void registerEvents() {
    CommandRegistrationCallback.EVENT.register(GitParcelModFabric::registerCommands);
  }

  private static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext context,
      Commands.CommandSelection commandSelection) {
    GitParcelMod.registerCommands(dispatcher, commandSelection, context);
  }
}
