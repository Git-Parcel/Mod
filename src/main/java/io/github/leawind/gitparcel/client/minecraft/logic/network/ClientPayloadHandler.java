package io.github.leawind.gitparcel.client.minecraft.logic.network;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcels.UpdateParcelsS2CPayload;
import org.slf4j.Logger;

/** Applies decoded server payloads to client-owned state. */
public final class ClientPayloadHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private ClientPayloadHandler() {}

  public static void handle(UpdateParcelFormatSpecS2CPayload payload) {
    LOGGER.debug("Update parcel format specs: {}", payload.specs());
    GitParcelClientImpl.INSTANCE.setParcelFormatSpecs(payload.specs());
  }

  public static void handle(UpdateParcelsS2CPayload payload) {
    var parcels = GitParcelClientImpl.INSTANCE.getParcels();
    if (payload.isFullSync()) {
      parcels.clear();
    } else {
      parcels.removeAll(payload.removedUuids());
    }

    parcels.putAll(payload.parcels());
  }
}
