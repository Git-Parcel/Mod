/*? if fabric {*/
package io.github.leawind.gitparcel.client.mc.platform.fabric;

import io.github.leawind.gitparcel.client.mc.GitParcelClientOptions;
import io.github.leawind.gitparcel.client.mc.ModClientEntrypoint;
import io.github.leawind.gitparcel.core.util.anno.VersionSensitive;
import net.fabricmc.api.ClientModInitializer;
/*? if >= 26.1 {*/
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/*?} else {*/
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
/*?}*/

public class ClientEntrypoint implements ClientModInitializer {
  @VersionSensitive("fabric keybinding -> keymapping")
  @Override
  public void onInitializeClient() {
    ModClientEntrypoint.initialize();
    // Register key mappings
    /*? if >= 26.1 {*/
    GitParcelClientOptions.registerKeyMappings(KeyMappingHelper::registerKeyMapping);
    /*?} else {*/
    /*GitParcelClientOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);*/
    /*?}*/
  }
}
/*?}*/
