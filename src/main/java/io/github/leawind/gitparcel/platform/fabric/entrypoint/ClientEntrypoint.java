/*? if fabric {*/
package io.github.leawind.gitparcel.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.core.client.GitParcelOptions;
import io.github.leawind.gitparcel.entrypoint.ModClientEntrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class ClientEntrypoint implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ModClientEntrypoint.initialize();
    // Register key mappings
    GitParcelOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
  }
}
/*?}*/
