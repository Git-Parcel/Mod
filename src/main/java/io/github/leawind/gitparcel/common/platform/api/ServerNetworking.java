package io.github.leawind.gitparcel.common.platform.api;

import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-independent server networking operations used by the common implementation.
 *
 * <p>Minecraft payload adaptation, registration, and reception remain behind the runtime and
 * loader boundaries. Common code only exchanges project-owned messages.
 */
public interface ServerNetworking {

  void send(ServerPlayer player, ServerMessage message);

  default void sendToAllPlayers(ServerLevel level, ServerMessage message) {
    level.players().forEach(player -> send(player, message));
  }
}
