/*? if fabric {*/
package io.github.leawind.gitparcel.mc.platform.fabric.entrypoint;

import io.github.leawind.gitparcel.mc.entrypoint.ModEntrypoint;
import io.github.leawind.gitparcel.mc.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.mc.network.protocol.parcels.UpdateParcelsS2CPayload;
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

  private static void registerPayloads() {
    PayloadTypeRegistry.playS2C()
        .register(
            UpdateParcelFormatSpecS2CPayload.TYPE, UpdateParcelFormatSpecS2CPayload.STREAM_CODEC);

    PayloadTypeRegistry.playS2C()
        .register(UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC);
  }
}
/*?}*/
