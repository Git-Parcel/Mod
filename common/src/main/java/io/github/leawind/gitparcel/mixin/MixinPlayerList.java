package io.github.leawind.gitparcel.mixin;

import io.github.leawind.gitparcel.server.GameServerApi;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(PlayerList.class)
public class MixinPlayerList {
  @Inject(method = "placeNewPlayer", at = @At("RETURN"))
  private void firePlayerJoinEvent(
      Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
    GameServerApi.ON_PLAYER_JOIN.emit(
        new GameServerApi.PlayerJoinEvent(connection, player, cookie));
  }
}
