package io.github.leawind.gitparcel.client.minecraft.logic.network;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelFormatsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import org.slf4j.Logger;

/** Applies decoded server payloads to client-owned state. */
public final class ClientPayloadHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private ClientPayloadHandler() {}

  public static void handle(UpdateParcelFormatsMessage message) {
    LOGGER.debug("Update parcel format capabilities: {}", message.capabilities());
    GitParcelClientImpl.INSTANCE.setParcelFormatCapabilities(message.capabilities());
  }

  public static void handle(UpdateParcelsMessage message) {
    message.applyTo(GitParcelClientImpl.INSTANCE.getParcels());
  }

  public static void handle(UpdateSharedRepositoriesMessage message) {
    GitParcelClientImpl.INSTANCE.setSharedRepositories(message);
  }

  public static void handle(UpdateGitOperationsMessage message) {
    GitParcelClientImpl.INSTANCE.setGitOperations(message);
  }
}
