package io.github.leawind.gitparcel.client.minecraft.logic.renderer;

import io.github.leawind.gitparcel.client.minecraft.bridge.GameClientApi;

public final class GitParcelRenderer {
  private GitParcelRenderer() {}

  public static final GitParcelRenderer INSTANCE = new GitParcelRenderer();

  private final ParcelRenderer parcelRenderer = new ParcelRenderer();

  public void renderGizmos(GameClientApi.Render.Context context) {
    parcelRenderer.renderGizmos(context);
  }
}
