package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.gui.screens.GitParcelDebugScreen;
import io.github.leawind.gitparcel.network.payload.UpdateParcelFormatInfosS2CPayload;
import net.minecraft.network.chat.Component;
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
                minecraft.setScreen(
                    new GitParcelDebugScreen(Component.literal("Git Parcel Debug Screen")));
              }
            }
          }
        });
  }
}
