/*? if fabric {*/
package io.github.leawind.gitparcel.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.entrypoint.ModServerEntrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;

public class ServerEntrypoint implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    ModServerEntrypoint.initialize();
  }
}
/*?}*/
