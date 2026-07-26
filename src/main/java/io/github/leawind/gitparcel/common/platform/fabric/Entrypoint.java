/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ServerQueryHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Entrypoint implements ModInitializer {
  @Override
  public void onInitialize() {
    ModEntrypoint.initialize();
    initialize();
  }

  private static void initialize() {
    registerPayloads();
    ModEntrypoint.registerCommandArgumentTypes(new CommandArgumentTypeRegistrarImpl());

    CommandRegistrationCallback.EVENT.register(ModEntrypoint::registerCommands);
    ServerPlayConnectionEvents.JOIN.register(
        (listener, sender, server) -> ModEntrypoint.onPlayerJoin(listener.getPlayer()));
    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
        (player, origin, destination) -> ModEntrypoint.onPlayerChangeDimension(player));
    ServerLifecycleEvents.SERVER_STARTED.register(ModEntrypoint::onServerStarted);
    ServerLifecycleEvents.SERVER_STOPPING.register(ModEntrypoint::onServerStopping);
  }

  @VersionSensitive("fabric playS2C -> clientboundPlay, since mc26.1")
  private static void registerPayloads() {
    /*? if >= 26.1 {*/
    PayloadTypeRegistry.clientboundPlay()
        .register(
            MinecraftPayloads.PARCEL_FORMATS_TYPE, MinecraftPayloads.PARCEL_FORMATS_CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(
            MinecraftPayloads.SHARED_REPOSITORIES_TYPE,
            MinecraftPayloads.SHARED_REPOSITORIES_CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MinecraftPayloads.OPERATIONS_TYPE, MinecraftPayloads.OPERATIONS_CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(MinecraftPayloads.PARCEL_HISTORY_TYPE, MinecraftPayloads.PARCEL_HISTORY_CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(
            MinecraftPayloads.QUERY_SERVER_STATE_TYPE,
            MinecraftPayloads.QUERY_SERVER_STATE_CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(
            MinecraftPayloads.QUERY_PARCEL_HISTORY_TYPE,
            MinecraftPayloads.QUERY_PARCEL_HISTORY_CODEC);
    /*?} else {*/
    /*PayloadTypeRegistry.playS2C()
                       .register(
                         MinecraftPayloads.PARCEL_FORMATS_TYPE, MinecraftPayloads.PARCEL_FORMATS_CODEC);
    PayloadTypeRegistry.playS2C()
                       .register(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
    */
    /*?}*/
    ServerPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.QUERY_SERVER_STATE_TYPE,
        (payload, context) ->
            context.server().execute(
                () -> ServerQueryHandler.handle(payload.message(), context.player())));
    ServerPlayNetworking.registerGlobalReceiver(
        MinecraftPayloads.QUERY_PARCEL_HISTORY_TYPE,
        (payload, context) ->
            context.server().execute(
                () -> ServerQueryHandler.handle(payload.message(), context.player())));
  }
}
/*?}*/
