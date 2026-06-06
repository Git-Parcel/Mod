/*? if fabric {*/
package io.github.leawind.gitparcel.mc.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.mc.entrypoint.ModServerEntrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ServerEntrypoint implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    ModServerEntrypoint.initialize();
  }
}
/*?}*/
