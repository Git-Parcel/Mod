package io.github.leawind.gitparcel.client;

import io.github.leawind.inventory.event.EventEmitter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class GameClientApi {
  public static final EventEmitter<Minecraft> ON_CLIENT_TICK_START = new EventEmitter<>();

  public static final class Network {
    public static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Minecraft>>
        ON_HANDLE_CUSTOM_PAYLOAD = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type, BiConsumer<T, Minecraft> handler) {
      ON_HANDLE_CUSTOM_PAYLOAD.put(type, (BiConsumer<CustomPacketPayload, Minecraft>) handler);
    }
  }
}
