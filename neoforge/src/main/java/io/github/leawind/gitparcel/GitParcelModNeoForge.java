package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcelinstance.UpdateParcelsS2CPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(GitParcelMod.MOD_ID)
public class GitParcelModNeoForge {

  public GitParcelModNeoForge(IEventBus eventBus) {
    GitParcelMod.init();
    GitParcelModNeoForge.init();
  }

  public static void init() {}

  @EventBusSubscriber(modid = GitParcelMod.MOD_ID)
  public static class EventHandler {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
      GitParcelMod.registerCommands(
          event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
      GitParcelMod.LOGGER.warn("Register payload handlers");
      var registrar = event.registrar(GitParcelMod.MOD_ID).versioned(GitParcelMod.PROTOCOL_VERSION);

      registrar.playToClient(
          UpdateParcelFormatInfosS2CPayload.TYPE,
          UpdateParcelFormatInfosS2CPayload.STREAM_CODEC,
          (a, b) -> {});

      registrar.playToClient(
          UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC, (a, b) -> {});
    }
  }
}
