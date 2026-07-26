/*? if fabric {*/
package io.github.leawind.gitparcel.common.platform.fabric;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ClientMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.payload.MinecraftPayloads;
import io.github.leawind.gitparcel.common.platform.api.ClientNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@AutoService(ClientNetworking.class)
public final class ClientNetworkingImpl implements ClientNetworking {
  @Override
  public void send(ClientMessage message) {
    ClientPlayNetworking.send(MinecraftPayloads.encode(message));
  }
}
/*?}*/
