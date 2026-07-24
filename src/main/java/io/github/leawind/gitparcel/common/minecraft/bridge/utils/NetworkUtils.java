package io.github.leawind.gitparcel.common.minecraft.bridge.utils;

import io.github.leawind.gitparcel.common.platform.api.Services;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;

public final class NetworkUtils {
  private NetworkUtils() {}

  public static void sendToAllPlayers(ServerLevel level, CustomPacketPayload payload) {
    Services.SERVER_NETWORKING.sendToAllPlayers(level, payload);
  }
}
