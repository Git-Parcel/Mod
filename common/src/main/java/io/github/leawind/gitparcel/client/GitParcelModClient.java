package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.gui.screens.GitParcelDebugScreen;
import io.github.leawind.gitparcel.client.renderer.ParcelInstanceBoundingBoxRenderer;
import io.github.leawind.gitparcel.network.protocol.parcelformat.ClientParcelFormatInfos;
import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcelinstance.UpdateParcelInstancesS2CPayload;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GitParcelModClient {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Cache of the parcel format infos received from the server.
   *
   * <p>Updated when received {@link UpdateParcelFormatInfosS2CPayload}.
   *
   * <p>Better be set to null when the client disconnects from the server.
   */
  public static @Nullable volatile ClientParcelFormatInfos PARCEL_FORMAT_INFOS = null;

  public static volatile List<ParcelInstance> PARCEL_INSTANCES = List.of();

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
          UpdateParcelFormatInfosS2CPayload.TYPE,
          (payload, minecraft) ->
              UpdateParcelFormatInfosS2CPayload.handle(payload, minecraft.player));

      GameClientApi.Network.registerGlobalReceiver(
          UpdateParcelInstancesS2CPayload.TYPE,
          (payload, minecraft) ->
              UpdateParcelInstancesS2CPayload.handle(payload, minecraft.player));
    }

    // Render
    {
      GameClientApi.Render.ON_BEFORE_TRANSLUCENT.on(
          (context) -> {
            if (!context.isInitialized()) {
              return;
            }

            ParcelInstanceBoundingBoxRenderer.INSTANCE.extractAndDrawWaypoint(context);
          });

      GameClientApi.Render.ON_GAME_RENDERER_CLOSE.on(
          ParcelInstanceBoundingBoxRenderer.INSTANCE::close);
    }
  }
}
