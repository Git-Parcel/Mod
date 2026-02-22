package io.github.leawind.gitparcel.mixin;

import io.github.leawind.gitparcel.GameEvents;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(ArgumentTypeInfos.class)
public class MixinArgumentTypeInfos {
  @Inject(
      at = @At("RETURN"),
      method =
          "bootstrap(Lnet/minecraft/core/Registry;)Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;")
  private static void bootstrap(
      Registry<ArgumentTypeInfo<?, ?>> registry,
      CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
    GameEvents.registerCommandArgumentTypes(registry);
  }
}
