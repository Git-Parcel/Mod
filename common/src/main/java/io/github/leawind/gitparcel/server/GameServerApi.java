package io.github.leawind.gitparcel.server;

import io.github.leawind.inventory.event.EventEmitter;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

public class GameServerApi {
  public record PlayerJoinEvent(
      Connection connection, ServerPlayer player, CommonListenerCookie cookie) {}

  public static final EventEmitter<PlayerJoinEvent> ON_PLAYER_JOIN = new EventEmitter<>();
}
