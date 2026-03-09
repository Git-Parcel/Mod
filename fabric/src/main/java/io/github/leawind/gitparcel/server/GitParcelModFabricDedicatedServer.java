package io.github.leawind.gitparcel.server;

import net.fabricmc.api.DedicatedServerModInitializer;

public class GitParcelModFabricDedicatedServer implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    GitParcelModDedicatedServer.init();
  }
}
