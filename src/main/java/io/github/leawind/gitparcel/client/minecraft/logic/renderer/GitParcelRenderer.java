package io.github.leawind.gitparcel.client.minecraft.logic.renderer;

public final class GitParcelRenderer {
  public static final GitParcelRenderer INSTANCE = new GitParcelRenderer();

  private final ParcelRenderer parcelRenderer = new ParcelRenderer();

  public void renderGizmos() {
    parcelRenderer.renderGizmos();
  }
}
