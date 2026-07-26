package io.github.leawind.gitparcel.client.platform.fabric;

/*? if fabric {*/
import io.github.leawind.gitparcel.client.minecraft.logic.GitParcelClientOptions;
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.client.minecraft.logic.network.ClientPayloadHandler;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
        MinecraftPayloads.PARCEL_FORMATS_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.PARCELS_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.SHARED_REPOSITORIES_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.GIT_OPERATIONS_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.PARCEL_HISTORY_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayConnectionEvents.DISCONNECT.register(
        (listener, client) -> ModClientEntrypoint.onDisconnect());

    // Register key mappings
    /*? if >= 26.1 {*/
    GitParcelClientOptions.registerKeyMappings(KeyMappingHelper::registerKeyMapping);
    /*?} else {*/
    /*GitParcelClientOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);*/
    /*?}*/
  }
}
/*?}*/
