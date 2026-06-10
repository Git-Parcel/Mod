package io.github.leawind.gitparcel.core.mc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.leawind.gitparcel.client.mc.GameClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
  @Unique private static final String INJECT_METHOD = "lambda$addMainPass$0";

  @Shadow @Final private Minecraft minecraft;
  @Shadow @Final private LevelRenderState levelRenderState;
  @Shadow private @Nullable ClientLevel level;

  @Unique
  private final GameClientApi.Render.Context gitparcel$context = new GameClientApi.Render.Context();

  @Unique
  private void doBeforeFinalizeGizmoCollection(PoseStack matrices) {
    gitparcel$context.prepare(minecraft, level, levelRenderState, matrices);
    if (gitparcel$context.isInitialized()) {
      GameClientApi.Render.ON_BEFORE_FINALIZE_GIZMOS.emit(gitparcel$context);
    }
  }

  @Inject(
      method = INJECT_METHOD,
      require = 0,
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
  private void beforeFinalizeGizmoCollection_fabric(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeFinalizeGizmoCollection(matrices);
  }
}
