package io.github.leawind.gitparcel.client.minecraft.logic;

import com.mojang.logging.LogUtils;
import icyllis.modernui.mc.MuiModApi;
import io.github.leawind.gitparcel.client.minecraft.bridge.GameClientApi;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.client.minecraft.logic.mui.debug.GitParcelDebugFragment;
import io.github.leawind.gitparcel.client.minecraft.logic.renderer.GitParcelRenderer;
import io.github.leawind.gitparcel.common.platform.api.Services;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public final class ModClientEntrypoint {
  private ModClientEntrypoint() {}

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void initialize() {
    LOGGER.debug("Initializing Git Parcel mod client");

    GameClientApi.Render.ON_BEFORE_FINALIZE_GIZMOS.on(
        (context) -> GitParcelRenderer.INSTANCE.renderGizmos());
  }

  public static void onClientTick(Minecraft minecraft) {
    if (!Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      return;
    }
    while (GitParcelClientOptions.keyDebugScreen.consumeClick()) {
      if (minecraft.player == null) {
        continue;
      }
      minecraft.setScreen(
          MuiModApi.get().createScreen(new GitParcelDebugFragment(), null, minecraft.screen));
    }
  }

  /** Clears state owned by the previous server connection. */
  public static void onDisconnect() {
    GitParcelClientImpl.INSTANCE.reset();
  }
}
