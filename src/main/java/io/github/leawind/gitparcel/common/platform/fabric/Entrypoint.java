/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.fabricmc.api.ModInitializer;
/*? if >=1.20.5 {*/
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
/*?}*/
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
/*? if >=1.20.4 {*/
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
/*?} else {*/
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerPlayer;
 *//*?}*/
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class Entrypoint implements ModInitializer {
  @Override
  public void onInitialize() {
    ModEntrypoint.initialize();
    initialize();
  }

  private static void initialize() {
    /*? if >=1.20.5 {*/
    registerPayloads();
    /*?}*/
    ModEntrypoint.registerCommandArgumentTypes(new CommandArgumentTypeRegistrarImpl());

    CommandRegistrationCallback.EVENT.register(ModEntrypoint::registerCommands);
    ServerPlayConnectionEvents.JOIN.register(
        (listener, sender, server) -> ModEntrypoint.onPlayerJoin(listener.getPlayer()));
    /*? if >=1.20.4 {*/
    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
        (player, origin, destination) -> ModEntrypoint.onPlayerChangeDimension(player));
    /*?} else {*/
    /*ServerEntityEvents.ENTITY_LOAD.register(
        (entity, world) -> {
          if (entity instanceof ServerPlayer player) {
            ModEntrypoint.onPlayerChangeDimension(player);
          }
        });
    *//*?}*/
    ServerLifecycleEvents.SERVER_STARTED.register(ModEntrypoint::onServerStarted);
    ServerLifecycleEvents.SERVER_STOPPING.register(ModEntrypoint::onServerStopping);
  }

  /*? if >=26.1 {*/
  @VersionSensitive("fabric playS2C -> clientboundPlay, since mc26.1")
  private static void registerPayloads() {
    PayloadTypeRegistry.clientboundPlay()
        .register(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
  }
  /*?} else if >=1.20.5 {*/
  /*@VersionSensitive("fabric playS2C -> clientboundPlay, since mc26.1")
  private static void registerPayloads() {
    PayloadTypeRegistry.playS2C()
                       .register(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
  }
  *//*?}*/
}
/*?}*/
