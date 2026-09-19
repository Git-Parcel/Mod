package io.github.leawind.gitparcel.client.minecraft.bridge;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.leawind.inventory.event.SimpleEventEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
/*? if >=26.1 {*/
import net.minecraft.client.renderer.state.level.LevelRenderState;
/*?}*/
import org.jspecify.annotations.Nullable;

public final class GameClientApi {
  private GameClientApi() {}

  public static final class Render {

    public static final SimpleEventEmitter.Owned<Context> ON_BEFORE_FINALIZE_GIZMOS =
        SimpleEventEmitter.create();

    public static final class Context {
      private boolean isInitialized = false;

      public Minecraft minecraft;
      public @Nullable ClientLevel level;
      /*? if >=26.1 {*/
      public LevelRenderState renderState;
      /*?}*/
      public PoseStack matrices;

      public void prepare(
          Minecraft minecraft,
          @Nullable ClientLevel level,
          /*? if >=26.1 {*/
          LevelRenderState renderState,
          /*?}*/
          PoseStack matrices) {
        this.minecraft = minecraft;
        this.level = level;
        /*? if >=26.1 {*/
        this.renderState = renderState;
        /*?}*/
        this.matrices = matrices;

        isInitialized = true;
      }

      public boolean isInitialized() {
        return isInitialized;
      }
    }
  }
}
