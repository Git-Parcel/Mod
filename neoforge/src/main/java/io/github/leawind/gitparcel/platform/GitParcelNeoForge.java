package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcels.UpdateParcelsS2CPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(GitParcel.MOD_ID)
public final class GitParcelNeoForge {

  public GitParcelNeoForge(IEventBus eventBus) {
    GitParcel.init();
    GitParcelNeoForge.init();
  }

  public static void init() {}

  @EventBusSubscriber(modid = GitParcel.MOD_ID)
  public static class EventHandler {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
      GitParcel.registerCommands(
          event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
      GitParcel.LOGGER.debug("Register payload handlers");
      var registrar = event.registrar(GitParcel.MOD_ID).versioned(GitParcel.PROTOCOL_VERSION);

      registrar.playToClient(
          UpdateParcelFormatInfosS2CPayload.TYPE,
          UpdateParcelFormatInfosS2CPayload.STREAM_CODEC,
          (a, b) -> {});

      registrar.playToClient(
          UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC, (a, b) -> {});
    }
  }
}
