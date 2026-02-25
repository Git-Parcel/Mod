package io.github.leawind.gitparcel;

import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class GitParcelModEntry {

  public GitParcelModEntry() {
    GitParcelMod.init();
  }
}
