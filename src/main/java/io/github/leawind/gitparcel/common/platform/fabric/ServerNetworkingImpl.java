/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.platform.api.ServerNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

@AutoService(ServerNetworking.class)
public final class ServerNetworkingImpl implements ServerNetworking {

  @Override
  public void send(ServerPlayer player, CustomPacketPayload payload) {
    ServerPlayNetworking.send(player, payload);
  }
}
/*?}*/
