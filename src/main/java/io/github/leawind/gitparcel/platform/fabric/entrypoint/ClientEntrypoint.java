/*? if fabric {*/
package io.github.leawind.gitparcel.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.entrypoint.ModClientEntrypoint;
import net.fabricmc.api.ClientModInitializer;

public class ClientEntrypoint implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ModClientEntrypoint.initialize();
  }
}
/*?}*/
