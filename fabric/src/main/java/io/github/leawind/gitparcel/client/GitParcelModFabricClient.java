package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.network.payload.UpdateParcelFormatInfosS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;

public class GitParcelModFabricClient implements ClientModInitializer {
  public static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void onInitializeClient() {
    GitParcelModClient.init();
    GitParcelModFabricClient.init();
  }

  public static void init() {
    // NOW forge, neoforge
    ClientPlayNetworking.registerGlobalReceiver(
        UpdateParcelFormatInfosS2CPacket.TYPE,
        (payload, context) -> {
          LOGGER.info("Received update parcel format infos packet: {}", payload.formats());
          ClientParcelFormatInfos.CACHE = payload.formats();
        });
  }
}
