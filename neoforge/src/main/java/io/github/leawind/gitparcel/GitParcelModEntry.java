package io.github.leawind.gitparcel;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class GitParcelModEntry {

  public GitParcelModEntry(IEventBus eventBus) {
    GitParcelMod.init();
  }
}
