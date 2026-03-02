package io.github.leawind.gitparcel;

import net.fabricmc.api.DedicatedServerModInitializer;

public class GitParcelModFabricServer implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    GitParcelModServer.init();
  }
}
