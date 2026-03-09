package io.github.leawind.gitparcel.client;

import net.fabricmc.api.ClientModInitializer;

public class GitParcelModFabricClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    GitParcelModClient.init();
    GitParcelModFabricClient.init();
  }

  public static void init() {}
}
