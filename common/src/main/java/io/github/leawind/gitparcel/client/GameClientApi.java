package io.github.leawind.gitparcel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.leawind.inventory.event.EventEmitter;
import io.github.leawind.inventory.event.SimpleEventEmitter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.Nullable;

public final class GameClientApi {
  public static final EventEmitter<Minecraft> ON_CLIENT_TICK_START = new EventEmitter<>();

  public static class Network {
    public static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Minecraft>>
        CUSTOM_PAYLOAD_HANDLERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
        CustomPacketPayload.Type<T> type, BiConsumer<T, Minecraft> handler) {
      CUSTOM_PAYLOAD_HANDLERS.put(type, (BiConsumer<CustomPacketPayload, Minecraft>) handler);
    }
  }

  public static final class Render {

    public static final SimpleEventEmitter<Context> ON_BEFORE_FINALIZE_GIZMOS =
        new SimpleEventEmitter<>();
    public static final SimpleEventEmitter<Context> ON_BEFORE_TRANSLUCENT =
        new SimpleEventEmitter<>();
    public static final SimpleEventEmitter<Void> ON_GAME_RENDERER_CLOSE =
        new SimpleEventEmitter<>();

    public static final class Context {
      private boolean isInitialized = false;

      public Minecraft minecraft;
      public @Nullable ClientLevel level;
      public LevelRenderState renderState;
      public PoseStack matrices;

      public void prepare(
          Minecraft minecraft,
          @Nullable ClientLevel level,
          LevelRenderState renderState,
          PoseStack matrices) {
        this.minecraft = minecraft;
        this.level = level;
        this.renderState = renderState;
        this.matrices = matrices;

        isInitialized = true;
      }

      public boolean isInitialized() {
        return isInitialized;
      }
    }
  }
}
