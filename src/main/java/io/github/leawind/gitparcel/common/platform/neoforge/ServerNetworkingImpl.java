package io.github.leawind.gitparcel.common.platform.neoforge;

/*? if neoforge {*/
/*
import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.platform.api.ServerNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

@AutoService(ServerNetworking.class)
public final class ServerNetworkingImpl implements ServerNetworking {

  @Override
  public void send(ServerPlayer player, ServerMessage message) {
    PacketDistributor.sendToPlayer(player, MinecraftPayloads.encode(message));
  }
}
*//*?}*/
