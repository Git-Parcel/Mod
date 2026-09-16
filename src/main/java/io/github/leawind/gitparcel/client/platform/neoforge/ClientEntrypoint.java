package io.github.leawind.gitparcel.client.platform.neoforge;

/*? if neoforge {*/
/*
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.client.minecraft.logic.network.ClientPayloadHandler;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GitParcel.MOD_ID, dist = Dist.CLIENT)
public class ClientEntrypoint {
  public ClientEntrypoint(IEventBus eventBus) {
    ModClientEntrypoint.initialize();

    eventBus.addListener(
        RegisterClientPayloadHandlersEvent.class, ClientEntrypoint::onRegisterPayloadHandlers);
    NeoForge.EVENT_BUS.addListener(
        ClientPlayerNetworkEvent.LoggingOut.class, ClientEntrypoint::onLoggingOut);
  }

  private static void onRegisterPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
    event.register(
        MinecraftPayloads.PARCELS_TYPE,
        (payload, context) -> ClientPayloadHandler.handle(payload.message()));
  }

  private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    ModClientEntrypoint.onDisconnect();
  }
}
*//*?}*/
