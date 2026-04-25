package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.gui.screens.GitParcelDebugScreen;
import io.github.leawind.gitparcel.client.renderer.GitParcelRenderer;
import io.github.leawind.gitparcel.network.protocol.parcelformat.ParcelFormatSpecs;
import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcels.UpdateParcelsS2CPayload;
import io.github.leawind.gitparcel.world.Parcels;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class GitParcelClient {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Cache of the parcel format specs received from the server.
   *
   * <p>Updated when received {@link UpdateParcelFormatSpecS2CPayload}.
   *
   * <p>Better be set to null when the client disconnects from the server.
   */
  public static @Nullable volatile ParcelFormatSpecs PARCEL_FORMAT_SPECS = null;

  public static volatile Parcels PARCELS = new Parcels();

  /**
   * Initializes the Git Parcel mod client.
   *
   * <p>This method is called on the client side.
   */
  public static void init() {
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
