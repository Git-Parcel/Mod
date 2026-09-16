package io.github.leawind.gitparcel.client.minecraft.logic.renderer;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.phys.Vec3;

public final class ParcelRenderer {
  ParcelRenderer() {}

  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;

  private static final int X_COLOR = 0xFFFF0000; // red
  private static final int Y_COLOR = 0xFF00FF00; // green
  private static final int Z_COLOR = 0xFF0000FF; // blue

  private static final float WIREFRAME_LINE_WIDTH = 2.5F;

  private static final WeakHashMap<Parcel, ParcelRenderState> CACHE = new WeakHashMap<>();

  void renderGizmos() {
    for (var parcel : GitParcelClient.get().getParcels().values()) {
      var currentState = new ParcelRenderState(parcel);
      var cachedState = CACHE.get(parcel);

      if (cachedState == null || !cachedState.equals(currentState)) {
        cachedState = currentState;
        cachedState.generateGizmos();
        CACHE.put(parcel, cachedState);
      }

      for (var gizmo : cachedState.gizmos) {
        Gizmos.addGizmo(gizmo);
      }
    }
  }

  private static final class ParcelRenderState {
    final Vec3i size;
    final Vec3i anchor;
    final Parcel.Visual visual;
    final ParcelTransform transform;

    final List<Gizmo> gizmos = new ArrayList<>(15);

    private ParcelRenderState(Parcel parcel) {
      this(parcel.meta().size(), parcel.meta().anchor(), parcel.visual(), parcel.transform());
    }

    private ParcelRenderState(
        Vec3i size, Vec3i anchor, Parcel.Visual visual, ParcelTransform transform) {
      this.size = size;
      this.anchor = anchor;
      this.visual = visual;
      this.transform = transform;
    }

    void generateGizmos() {
      gizmos.clear();

      // The wireframe outlines the parcel extent expressed in anchor-relative coordinates; the
      // transform's translation is the anchor's absolute world position.
      var minCorner =
          new Vec3(-anchor.getX(), -anchor.getY(), -anchor.getZ());
      var maxCorner =
          new Vec3(
              size.getX() - anchor.getX(),
              size.getY() - anchor.getY(),
              size.getZ() - anchor.getZ());
      var pivot = transform.apply(minCorner);

      // Wireframe
      if (visual.showWireframe()) {
        var x = transform.apply(new Vec3(maxCorner.x, minCorner.y, minCorner.z));
        var y = transform.apply(new Vec3(minCorner.x, maxCorner.y, minCorner.z));
        var z = transform.apply(new Vec3(minCorner.x, minCorner.y, maxCorner.z));

        var xyz = transform.apply(maxCorner);
        var yz = transform.apply(new Vec3(minCorner.x, maxCorner.y, maxCorner.z));
        var xz = transform.apply(new Vec3(maxCorner.x, minCorner.y, maxCorner.z));
        var xy = transform.apply(new Vec3(maxCorner.x, maxCorner.y, minCorner.z));

        gizmos.add(new LineGizmo(pivot, x, X_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(pivot, y, Y_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(pivot, z, Z_COLOR, WIREFRAME_LINE_WIDTH));

        gizmos.add(new LineGizmo(yz, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(yz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));

        gizmos.add(new LineGizmo(xz, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(xz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));

        gizmos.add(new LineGizmo(xy, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(xy, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));

        gizmos.add(new LineGizmo(xyz, yz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(xyz, xz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
        gizmos.add(new LineGizmo(xyz, xy, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH));
      }

      if (visual.showAnchor()) {
        final float ANCHOR_SIZE = 1F;

        var anchorPos = new Vec3(transform.translation());
        var x = anchorPos.add(transform.applyVector(new Vec3(ANCHOR_SIZE, 0, 0)));
        var y = anchorPos.add(transform.applyVector(new Vec3(0, ANCHOR_SIZE, 0)));
        var z = anchorPos.add(transform.applyVector(new Vec3(0, 0, ANCHOR_SIZE)));

        gizmos.add(new ArrowGizmo(anchorPos, x, X_COLOR, WIREFRAME_LINE_WIDTH * 2));
        gizmos.add(new ArrowGizmo(anchorPos, y, Y_COLOR, WIREFRAME_LINE_WIDTH * 2));
        gizmos.add(new ArrowGizmo(anchorPos, z, Z_COLOR, WIREFRAME_LINE_WIDTH * 2));
      }
    }

    @Override
    public int hashCode() {
      int result = size.hashCode();
      result = 31 * result + anchor.hashCode();
      result = 31 * result + visual.hashCode();
      result = 31 * result + transform.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      var that = (ParcelRenderState) obj;
      return size.equals(that.size)
          && anchor.equals(that.anchor)
          && visual.equals(that.visual)
          && transform.equals(that.transform);
    }
  }
}
