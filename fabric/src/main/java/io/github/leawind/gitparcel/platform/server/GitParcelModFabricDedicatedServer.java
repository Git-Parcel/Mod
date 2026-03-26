package io.github.leawind.gitparcel.platform.server;

import io.github.leawind.gitparcel.server.GitParcelModDedicatedServer;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class GitParcelModFabricDedicatedServer implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    GitParcelModDedicatedServer.init();
    GitParcelModFabricDedicatedServer.init();
  }

  public static void init() {}
}
