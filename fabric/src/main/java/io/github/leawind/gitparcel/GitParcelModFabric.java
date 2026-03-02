package io.github.leawind.gitparcel;

import net.fabricmc.api.ModInitializer;

public class GitParcelModFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();
  }
}
