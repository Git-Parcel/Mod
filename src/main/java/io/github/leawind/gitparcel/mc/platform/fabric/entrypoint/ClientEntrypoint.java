/*? if fabric {*/
package io.github.leawind.gitparcel.mc.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.mc.client.GitParcelOptions;
import io.github.leawind.gitparcel.mc.entrypoint.ModClientEntrypoint;
import io.github.leawind.gitparcel.util.anno.VersionSensitive;
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
    GitParcelOptions.registerKeyMappings(KeyMappingHelper::registerKeyMapping);
    /*?} else {*/
    /*GitParcelOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);*/
    /*?}*/
  }
}
/*?}*/
