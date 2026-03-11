package io.github.leawind.gitparcel.client;

import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.network.payload.UpdateParcelFormatInfosS2CPayload;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@Mod(value = GitParcelMod.MOD_ID, dist = Dist.CLIENT)
public class GitParcelModNeoForgeClient {
  public GitParcelModNeoForgeClient(IEventBus eventBus) {
    GitParcelModClient.init();
    GitParcelModNeoForgeClient.init(eventBus);
  }

  public static void init(IEventBus eventBus) {}

  @EventBusSubscriber(modid = GitParcelMod.MOD_ID)
  public static class EventHandler {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      GitParcelOptions.registerKeyMappings(event::register);
    }

    @SubscribeEvent
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
      event.register(
          UpdateParcelFormatInfosS2CPayload.TYPE,
          HandlerThread.MAIN,
          (payload, context) ->
              UpdateParcelFormatInfosS2CPayload.handle(payload, (LocalPlayer) context.player()));
    }
  }
}
