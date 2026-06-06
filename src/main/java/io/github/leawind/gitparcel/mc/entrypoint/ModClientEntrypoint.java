package io.github.leawind.gitparcel.mc.entrypoint;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.mc.client.GameClientApi;
import io.github.leawind.gitparcel.mc.client.GitParcelOptions;
import io.github.leawind.gitparcel.mc.client.gui.screens.GitParcelDebugScreen;
import io.github.leawind.gitparcel.mc.client.renderer.GitParcelRenderer;
import io.github.leawind.gitparcel.mc.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.mc.network.protocol.parcels.UpdateParcelsS2CPayload;
import org.slf4j.Logger;

public class ModClientEntrypoint {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static void initialize() {
    LOGGER.debug("Initializing Git Parcel mod client");

    GameClientApi.ON_CLIENT_TICK_START.on(
        minecraft -> {
          if (!(minecraft.screen instanceof GitParcelDebugScreen)) {
            while (GitParcelOptions.keyDebugScreen.consumeClick()) {
              if (minecraft.player != null) {
                minecraft.setScreen(new GitParcelDebugScreen(minecraft.screen));
              }
            }
          }
        });

    // Network
    {
      GameClientApi.Network.registerGlobalReceiver(
          UpdateParcelFormatSpecS2CPayload.TYPE,
          (payload, minecraft) ->
              UpdateParcelFormatSpecS2CPayload.handle(payload, minecraft.player));

      GameClientApi.Network.registerGlobalReceiver(
          UpdateParcelsS2CPayload.TYPE,
          (payload, minecraft) -> UpdateParcelsS2CPayload.handle(payload, minecraft.player));
    }

    // Render
    {
      GameClientApi.Render.ON_BEFORE_FINALIZE_GIZMOS.on(
          (context) -> GitParcelRenderer.INSTANCE.renderGizmos());
    }
  }
}
