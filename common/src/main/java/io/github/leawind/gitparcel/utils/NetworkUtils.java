package io.github.leawind.gitparcel.utils;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;

public class NetworkUtils {
  public static void sendToAllPlayers(ServerLevel level, CustomPacketPayload payload) {
    var packet = new ClientboundCustomPayloadPacket(payload);
    level.players().forEach(player -> player.connection.send(packet));
  }
}
