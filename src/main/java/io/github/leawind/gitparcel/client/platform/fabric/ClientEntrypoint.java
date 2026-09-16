package io.github.leawind.gitparcel.client.platform.fabric;

/*? if fabric {*/
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.client.minecraft.logic.network.ClientPayloadHandler;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientEntrypoint implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ModClientEntrypoint.initialize();

    ClientPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.PARCELS_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
    ClientPlayConnectionEvents.DISCONNECT.register(
        (listener, client) -> ModClientEntrypoint.onDisconnect());
  }
}
/*?}*/
