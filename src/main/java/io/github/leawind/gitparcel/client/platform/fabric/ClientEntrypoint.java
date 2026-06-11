package io.github.leawind.gitparcel.client.platform.fabric;

/*? if fabric {*/
import io.github.leawind.gitparcel.client.minecraft.logic.GitParcelClientOptions;
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
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
