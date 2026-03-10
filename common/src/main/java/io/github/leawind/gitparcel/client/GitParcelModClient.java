package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.platform.Services;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class GitParcelModClient {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Initializes the Git Parcel mod client.
   *
   * <p>This method is called on the client side.
   */
  public static void init() {
    LOGGER.debug("Initializing Git Parcel mod client");

    LOGGER.debug("Registering key mappings");
    GitParcelOptions.registerAllKeyMappings(Services.PLATFORM);

    GameClientApi.ON_CLIENT_TICK_START.on(
        minecraft -> {
          while (GitParcelOptions.keyDebugScreen.consumeClick()) {
            if (minecraft.player != null) {
              minecraft.player.displayClientMessage(Component.literal("Key Pressed!"), false);
            }
          }
        });
  }
}
