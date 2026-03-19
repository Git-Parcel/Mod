package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcelinstance.UpdateParcelInstancesS2CPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class GitParcelModFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();
    GitParcelModFabric.init();
  }

  public static void init() {
    registerPayloads();

    // Register commands
    CommandRegistrationCallback.EVENT.register(GitParcelMod::registerCommands);
  }

  private static void registerPayloads() {
    PayloadTypeRegistry.playS2C()
        .register(
            UpdateParcelFormatInfosS2CPayload.TYPE, UpdateParcelFormatInfosS2CPayload.STREAM_CODEC);

    PayloadTypeRegistry.playS2C()
        .register(
            UpdateParcelInstancesS2CPayload.TYPE, UpdateParcelInstancesS2CPayload.STREAM_CODEC);
  }
}
