package io.github.leawind.gitparcel.server;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

public final class GameServerApi {
  public record PlayerJoinEvent(
      Connection connection, ServerPlayer player, CommonListenerCookie cookie) {}

  public static final SimpleEventEmitter<PlayerJoinEvent> ON_PLAYER_JOIN =
      new SimpleEventEmitter<>();
}
