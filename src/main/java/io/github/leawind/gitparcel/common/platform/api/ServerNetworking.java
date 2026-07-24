package io.github.leawind.gitparcel.common.platform.api;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-independent server networking operations used by the common implementation.
 *
 * <p>Payload registration and reception remain in the loader entrypoints. This service only
 * exposes the small set of sending operations needed by common code.
 */
public interface ServerNetworking {

  void send(ServerPlayer player, CustomPacketPayload payload);

  default void sendToAllPlayers(ServerLevel level, CustomPacketPayload payload) {
    level.players().forEach(player -> send(player, payload));
  }
}
