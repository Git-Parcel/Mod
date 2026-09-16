package io.github.leawind.gitparcel.client.minecraft.logic;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.minecraft.bridge.GameClientApi;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.client.minecraft.logic.renderer.GitParcelRenderer;
import org.slf4j.Logger;

public final class ModClientEntrypoint {
  private ModClientEntrypoint() {}

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void initialize() {
    LOGGER.debug("Initializing Git Parcel mod client");

    GameClientApi.Render.ON_BEFORE_FINALIZE_GIZMOS.on(
        (context) -> GitParcelRenderer.INSTANCE.renderGizmos());
  }

  /** Clears state owned by the previous server connection. */
  public static void onDisconnect() {
    GitParcelClientImpl.INSTANCE.reset();
  }
}
