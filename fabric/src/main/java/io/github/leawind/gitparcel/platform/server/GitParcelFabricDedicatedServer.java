package io.github.leawind.gitparcel.platform.server;

import io.github.leawind.gitparcel.server.GitParcelDedicatedServer;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class GitParcelFabricDedicatedServer implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    GitParcelDedicatedServer.init();
    GitParcelFabricDedicatedServer.init();
  }

  public static void init() {}
}
