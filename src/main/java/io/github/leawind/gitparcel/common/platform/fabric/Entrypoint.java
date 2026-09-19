/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

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
    PayloadTypeRegistry.clientboundPlay()
        .register(MinecraftPayloads.PARCELS_TYPE, MinecraftPayloads.PARCELS_CODEC);
  }
}
/*?}*/
