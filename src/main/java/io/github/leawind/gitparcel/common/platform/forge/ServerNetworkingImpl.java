package io.github.leawind.gitparcel.common.platform.forge;

/*? if forge {*/
/*
import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.client.minecraft.logic.network.ClientPayloadHandler;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.platform.api.ServerNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@AutoService(ServerNetworking.class)
public final class ServerNetworkingImpl implements ServerNetworking {
  private static final String PROTOCOL = String.valueOf(GitParcel.PROTOCOL_VERSION);
  private static final SimpleChannel CHANNEL =
      NetworkRegistry.newSimpleChannel(
          new ResourceLocation(GitParcel.MOD_ID, "main"),
          () -> PROTOCOL,
          PROTOCOL::equals,
          PROTOCOL::equals);

  static void registerPayload() {
    CHANNEL.messageBuilder(UpdateParcelsMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
        .encoder(MinecraftPayloads::write)
        .decoder(MinecraftPayloads::read)
        .consumerMainThread(
            (message, context) -> {
              ClientPayloadHandler.handle(message);
              context.get().setPacketHandled(true);
            })
        .add();
  }

  @Override
  public void send(ServerPlayer player, ServerMessage message) {
    CHANNEL.send(
        PacketDistributor.PLAYER.with(() -> player), (UpdateParcelsMessage) message);
  }
}
*//*?}*/
