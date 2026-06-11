/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcels.UpdateParcelsS2CPayload;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Entrypoint implements ModInitializer {
  @Override
  public void onInitialize() {
    ModEntrypoint.initialize();
    initialize();
  }

  private static void initialize() {
    registerPayloads();

    // Register commands
    CommandRegistrationCallback.EVENT.register(ModEntrypoint::registerCommands);
  }

  @VersionSensitive("fabric playS2C -> clientboundPlay, since mc26.1")
  private static void registerPayloads() {
    /*? if >= 26.1 {*/
    PayloadTypeRegistry.clientboundPlay()
        .register(
            UpdateParcelFormatSpecS2CPayload.TYPE, UpdateParcelFormatSpecS2CPayload.STREAM_CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC);
    /*?} else {*/
    /*PayloadTypeRegistry.playS2C()
                       .register(
                         UpdateParcelFormatSpecS2CPayload.TYPE, UpdateParcelFormatSpecS2CPayload.STREAM_CODEC);
    PayloadTypeRegistry.playS2C()
                       .register(UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC);
    */
    /*?}*/
  }
}
/*?}*/
