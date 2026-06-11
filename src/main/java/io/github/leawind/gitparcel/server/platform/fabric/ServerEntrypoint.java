/*? if fabric {*/
package io.github.leawind.gitparcel.server.platform.fabric;

import io.github.leawind.gitparcel.server.minecraft.logic.ModServerEntrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ServerEntrypoint implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    ModServerEntrypoint.initialize();
  }
}
/*?}*/
