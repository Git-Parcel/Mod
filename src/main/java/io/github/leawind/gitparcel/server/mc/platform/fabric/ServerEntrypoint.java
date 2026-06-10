/*? if fabric {*/
package io.github.leawind.gitparcel.server.mc.platform.fabric;

import io.github.leawind.gitparcel.server.mc.ModServerEntrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ServerEntrypoint implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    ModServerEntrypoint.initialize();
  }
}
/*?}*/
