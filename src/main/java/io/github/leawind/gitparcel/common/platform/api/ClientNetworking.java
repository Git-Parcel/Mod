package io.github.leawind.gitparcel.common.platform.api;

import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ClientMessage;

/** Loader-independent client networking operations, loaded only on the physical client. */
public interface ClientNetworking {
  void send(ClientMessage message);
}
