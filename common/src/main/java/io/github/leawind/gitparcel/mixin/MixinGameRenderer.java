package io.github.leawind.gitparcel.mixin;

import io.github.leawind.gitparcel.client.GameClientApi;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
  @Inject(method = "close", at = @At("RETURN"))
  private void onGameRendererClose(CallbackInfo ci) {
    GameClientApi.Render.ON_GAME_RENDERER_CLOSE.emit();
  }
}
