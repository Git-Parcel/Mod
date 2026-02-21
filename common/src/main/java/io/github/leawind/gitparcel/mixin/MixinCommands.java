package io.github.leawind.gitparcel.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.api.GameEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class MixinCommands {

  @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

  @Inject(
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"),
      method = "<init>")
  private void fabric_addCommands(
      Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
    GameEvents.REGISTER_COMMANDS.accept(
        new GameEvents.RegiterCommands(dispatcher, selection, context));
  }
}
