/*? if fabric {*/
package io.github.leawind.gitparcel.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.entrypoint.ModEntrypoint;
import net.fabricmc.api.ModInitializer;

public class Entrypoint implements ModInitializer {
  @Override
  public void onInitialize() {
    ModEntrypoint.initialize();
  }
}
/*?}*/
