package io.github.leawind.gitparcel.client.platform.fabric;

/*? if fabric {*/
import io.github.leawind.gitparcel.client.minecraft.logic.GitParcelClientOptions;
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.client.minecraft.logic.network.ClientPayloadHandler;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcels.UpdateParcelsS2CPayload;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    ClientTickEvents.START_CLIENT_TICK.register(ModClientEntrypoint::onClientTick);
    ClientPlayNetworking.registerGlobalReceiver(
        UpdateParcelFormatSpecS2CPayload.TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(
        UpdateParcelsS2CPayload.TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload));

    // Register key mappings
    /*? if >= 26.1 {*/
    GitParcelClientOptions.registerKeyMappings(KeyMappingHelper::registerKeyMapping);
    /*?} else {*/
    /*GitParcelClientOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);*/
    /*?}*/
  }
}
/*?}*/
