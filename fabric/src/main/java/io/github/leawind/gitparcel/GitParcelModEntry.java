package io.github.leawind.gitparcel;

import net.fabricmc.api.ModInitializer;

public class GitParcelModEntry implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();
  }
}
