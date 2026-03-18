package io.github.leawind.gitparcel.mixin;

import io.github.leawind.gitparcel.client.GameClientApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListenerImpl {
  @Shadow @Final protected Minecraft minecraft;

  @Inject(
      method =
          "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V",
      at = @At("HEAD"),
      cancellable = true)
  public void onCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
    var payload = packet.payload();
    var type = payload.type();
    var handler = GameClientApi.Network.ON_HANDLE_CUSTOM_PAYLOAD.get(type);

    if (handler == null) {
      return;
    }

    try {
      handler.accept(payload, minecraft);
    } catch (RunningOnDifferentThreadException e) {
      this.minecraft
          .packetProcessor()
          .scheduleIfPossible((ClientCommonPacketListenerImpl) (Object) this, packet);
    }

    ci.cancel();
  }
}
