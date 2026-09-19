package io.github.leawind.gitparcel.common.minecraft.bridge.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.leawind.gitparcel.client.minecraft.bridge.GameClientApi;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.client.Minecraft;
/*? if <26.3 {*/
/*import net.minecraft.client.multiplayer.ClientLevel;
 *//*?}*/
import net.minecraft.client.renderer.LevelRenderer;
/*? if >=26.1 {*/
import net.minecraft.client.renderer.state.level.LevelRenderState;
/*?}*/
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The gizmo collection hook moved across versions: 1.20.1 draws in the frame-graph-less
 * {@code renderLevel}, 26.1 injected into the {@code addMainPass} lambda, and 26.3 moved that hook
 * into {@code submitFeatures}, which also dropped the renderer's {@code minecraft} and {@code
 * level} fields in favour of the shared render state.
 */
@SuppressWarnings("unused")
@VersionSensitive("LevelRenderer render internals; keep this as the remaining render seam")
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
  /*? if >=26.3 {*/
  @Unique private static final String INJECT_METHOD = "submitFeatures";
  /*?} else if >=26.1 {*/
  /*@Unique private static final String INJECT_METHOD = "lambda$addMainPass$0";
  *//*?} else {*/
  /*@Unique private static final String INJECT_METHOD = "renderLevel";
  *//*?}*/

  /*? if >=26.3 {*/
  @Shadow @Final private LevelRenderState levelRenderState;
  /*?} else if >=26.1 {*/
  /*@Shadow @Final private Minecraft minecraft;
  @Shadow @Final private LevelRenderState levelRenderState;
  @Shadow private @Nullable ClientLevel level;
  *//*?} else {*/
  /*@Shadow @Final private Minecraft minecraft;
  @Shadow private @Nullable ClientLevel level;
  *//*?}*/

  @Unique
  private final GameClientApi.Render.Context gitparcel$context = new GameClientApi.Render.Context();

  @Unique
  private void doBeforeFinalizeGizmoCollection(PoseStack matrices) {
    /*? if >=26.3 {*/
    var minecraft = Minecraft.getInstance();
    gitparcel$context.prepare(minecraft, minecraft.level, levelRenderState, matrices);
    /*?} else if >=26.1 {*/
    /*gitparcel$context.prepare(minecraft, level, levelRenderState, matrices);
    *//*?} else {*/
    /*gitparcel$context.prepare(minecraft, level, matrices);
    *//*?}*/
    if (gitparcel$context.isInitialized()) {
      GameClientApi.Render.ON_BEFORE_FINALIZE_GIZMOS.emit(gitparcel$context);
    }
  }

  /*? if >=26.1 {*/
  @Inject(
      method = INJECT_METHOD,
      require = 0,
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V"))
  private void beforeFinalizeGizmoCollection(CallbackInfo ci, @Local PoseStack matrices) {
    doBeforeFinalizeGizmoCollection(matrices);
  }
  /*?} else {*/
  /*@Inject(method = INJECT_METHOD, at = @At("RETURN"))
  private void beforeRenderLevelEnd(PoseStack poseStack, CallbackInfo ci) {
    doBeforeFinalizeGizmoCollection(poseStack);
  }
  *//*?}*/
}
