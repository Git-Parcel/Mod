package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcels.UpdateParcelS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcels.UpdateParcelsS2CPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class GitParcelFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcel.init();
    GitParcelFabric.init();
  }

  public static void init() {
    registerPayloads();

    // Register commands
    CommandRegistrationCallback.EVENT.register(GitParcel::registerCommands);
  }

  private static void registerPayloads() {
    PayloadTypeRegistry.playS2C()
        .register(
            UpdateParcelFormatInfosS2CPayload.TYPE, UpdateParcelFormatInfosS2CPayload.STREAM_CODEC);

    PayloadTypeRegistry.playS2C()
        .register(UpdateParcelsS2CPayload.TYPE, UpdateParcelsS2CPayload.STREAM_CODEC);

    PayloadTypeRegistry.playS2C()
        .register(UpdateParcelS2CPayload.TYPE, UpdateParcelS2CPayload.STREAM_CODEC);
  }
}
