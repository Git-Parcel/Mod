/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.platform.api.ServerNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

@AutoService(ServerNetworking.class)
public final class ServerNetworkingImpl implements ServerNetworking {

  @Override
  public void send(ServerPlayer player, ServerMessage message) {
    ServerPlayNetworking.send(player, MinecraftPayloads.encode(message));
  }
}
/*?}*/
