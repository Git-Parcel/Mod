package io.github.leawind.gitparcel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class GitParcelModFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> {
          GitParcelMod.registerCommands(dispatcher, environment, registryAccess);
        });
  }
}
