package io.github.leawind.gitparcel.client.renderer;

public class GitParcelRenderer {
  public static final GitParcelRenderer INSTANCE = new GitParcelRenderer();

  private final ParcelRenderer parcelRenderer = new ParcelRenderer();

  public void render() {
    parcelRenderer.render();
  }
}
