package io.github.leawind.gitparcel.mixin;

import io.github.leawind.gitparcel.client.GameClientApi;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

  @Inject(at = @At("HEAD"), method = "tick")
  private void onStartTick(CallbackInfo info) {
    GameClientApi.ON_CLIENT_TICK_START.emit((Minecraft) (Object) this);
  }
}
