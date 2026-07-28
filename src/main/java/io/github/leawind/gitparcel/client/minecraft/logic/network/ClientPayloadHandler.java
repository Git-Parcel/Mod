package io.github.leawind.gitparcel.client.minecraft.logic.network;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelContentsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import org.slf4j.Logger;

/** Applies decoded server payloads to client-owned state. */
public final class ClientPayloadHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private ClientPayloadHandler() {}

  public static void handle(UpdateParcelContentsMessage message) {
    LOGGER.debug("Update parcel content capabilities: {}", message.capabilities());
    GitParcelClientImpl.INSTANCE.setParcelContentCapabilities(message.capabilities());
  }

  public static void handle(UpdateParcelsMessage message) {
    message.applyTo(GitParcelClientImpl.INSTANCE.getParcels());
    if (message.fullSync()) {
      GitParcelClientImpl.INSTANCE.clearParcelHistory();
    } else {
      GitParcelClientImpl.INSTANCE.removeParcelHistory(message.removedUuids());
    }
  }

  public static void handle(UpdateSharedRepositoriesMessage message) {
    GitParcelClientImpl.INSTANCE.setSharedRepositories(message);
  }

  public static void handle(UpdateOperationsMessage message) {
    GitParcelClientImpl.INSTANCE.setOperations(message);
  }

  public static void handle(UpdateParcelHistoryMessage message) {
    GitParcelClientImpl.INSTANCE.setParcelHistory(message);
  }
}
