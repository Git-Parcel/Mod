package io.github.leawind.gitparcel.common.platform.neoforge;

/*? if neoforge {*/
/*
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(GitParcel.MOD_ID)
public class Entrypoint {
  public Entrypoint(IEventBus modEventBus) {
    ModEntrypoint.initialize();

    var argumentTypes = new CommandArgumentTypeRegistrarImpl();
    ModEntrypoint.registerCommandArgumentTypes(argumentTypes);
    argumentTypes.attach(modEventBus);

    modEventBus.addListener(
        RegisterPayloadHandlersEvent.class, Entrypoint::onRegisterPayloadHandlers);
    NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, Entrypoint::onRegisterCommands);
    NeoForge.EVENT_BUS.addListener(
        PlayerEvent.PlayerLoggedInEvent.class, Entrypoint::onPlayerLoggedIn);
  }

  private static void onRegisterCommands(RegisterCommandsEvent event) {
    ModEntrypoint.registerCommands(
        event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
  }

  private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
    ModEntrypoint.LOGGER.debug("Register payload handlers");
    var registrar = event.registrar(GitParcel.MOD_ID).versioned(GitParcel.PROTOCOL_VERSION);

    registrar.playToClient(
        MinecraftPayloads.PARCEL_FORMATS_TYPE, MinecraftPayloads.PARCEL_FORMATS_CODEC);
    registrar.playToClient(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
  }

  private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ModEntrypoint.onPlayerJoin(player);
    }
  }
}
*//*?}*/
