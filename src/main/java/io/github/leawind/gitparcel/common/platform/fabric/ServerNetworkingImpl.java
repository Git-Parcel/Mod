/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.platform.api.ServerNetworking;
/*? if <26.1 {*/
/*import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
 *//*?}*/
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

@AutoService(ServerNetworking.class)
public final class ServerNetworkingImpl implements ServerNetworking {

  @Override
  public void send(ServerPlayer player, ServerMessage message) {
    /*? if >=26.1 {*/
    ServerPlayNetworking.send(player, MinecraftPayloads.encode(message));
    /*?} else {*/
    /*var update = MinecraftPayloads.encode(message);
    var buf = PacketByteBufs.create();
    MinecraftPayloads.write(update, buf);
    ServerPlayNetworking.send(player, MinecraftPayloads.PARCELS_ID, buf);
    *//*?}*/
  }
}
/*?}*/
