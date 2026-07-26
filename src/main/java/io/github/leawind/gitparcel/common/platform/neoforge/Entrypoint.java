package io.github.leawind.gitparcel.common.platform.neoforge;

/*? if neoforge {*/
/*
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ServerQueryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
    NeoForge.EVENT_BUS.addListener(
        PlayerEvent.PlayerChangedDimensionEvent.class, Entrypoint::onPlayerChangedDimension);
    NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, Entrypoint::onServerStopping);
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
    registrar.playToClient(
        MinecraftPayloads.SHARED_REPOSITORIES_TYPE,
        MinecraftPayloads.SHARED_REPOSITORIES_CODEC);
    registrar.playToClient(
        MinecraftPayloads.GIT_OPERATIONS_TYPE, MinecraftPayloads.GIT_OPERATIONS_CODEC);
    registrar.playToClient(
        MinecraftPayloads.PARCEL_HISTORY_TYPE, MinecraftPayloads.PARCEL_HISTORY_CODEC);
    registrar.playToServer(
        MinecraftPayloads.QUERY_SERVER_STATE_TYPE,
        MinecraftPayloads.QUERY_SERVER_STATE_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    ServerQueryHandler.handle(
                        payload.message(), (ServerPlayer) context.player())));
    registrar.playToServer(
        MinecraftPayloads.QUERY_PARCEL_HISTORY_TYPE,
        MinecraftPayloads.QUERY_PARCEL_HISTORY_CODEC,
        (payload, context) ->
            context.enqueueWork(
                () ->
                    ServerQueryHandler.handle(
                        payload.message(), (ServerPlayer) context.player())));
  }

  private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ModEntrypoint.onPlayerJoin(player);
    }
  }

  private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ModEntrypoint.onPlayerChangeDimension(player);
    }
  }

  private static void onServerStopping(ServerStoppingEvent event) {
    ModEntrypoint.onServerStopping(event.getServer());
  }
}
*//*?}*/
