package io.github.leawind.gitparcel.mc.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.leawind.gitparcel.mc.client.GameClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
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

  @Unique
  private void doBeforeTranslucentRender(PoseStack matrices) {
    gitparcel$context.prepare(minecraft, level, levelRenderState, matrices);
    GameClientApi.Render.ON_BEFORE_TRANSLUCENT.emit(gitparcel$context);
  }

  /*? if fabric {*/

  @Inject(
      method = "method_62214",
      require = 0,
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
  private void beforeFinalizeGizmoCollection_fabric(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeFinalizeGizmoCollection(matrices);
  }

  @Inject(
      method = "method_62214",
      require = 0,
      at =
          @At(
              value = "INVOKE_STRING",
              target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
              args = "ldc=translucent"))
  private void beforeTranslucentRender_fabric(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeTranslucentRender(matrices);
  }

  /*?}*/

  /*? if neoforge {*/
  /*@Inject(
      method = "lambda$addMainPass$1",
      require = 0,
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
  private void beforeFinalizeGizmoCollection_neoforge(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeFinalizeGizmoCollection(matrices);
  }

  @Inject(
      method = "lambda$addMainPass$1",
      require = 0,
      at =
          @At(
              value = "INVOKE_STRING",
              target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
              args = "ldc=translucent"))
  private void beforeTranslucentRender_neoforge(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeTranslucentRender(matrices);
  }
  */
  /*?}*/
}
