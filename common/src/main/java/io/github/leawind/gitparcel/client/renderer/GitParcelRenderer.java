package io.github.leawind.gitparcel.client.renderer;

public class GitParcelRenderer {
  public static final GitParcelRenderer INSTANCE = new GitParcelRenderer();

  private final ParcelInstanceRenderer parcelInstanceRenderer = new ParcelInstanceRenderer();

  public void render() {
    parcelInstanceRenderer.render();
  }
}
