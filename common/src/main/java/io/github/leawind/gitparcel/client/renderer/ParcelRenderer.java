package io.github.leawind.gitparcel.client.renderer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.world.phys.Vec3;

public final class ParcelRenderer {
  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;

  private static final int X_COLOR = 0xFFFF0000; // red
  private static final int Y_COLOR = 0xFF00FF00; // green
  private static final int Z_COLOR = 0xFF0000FF; // blue

  private static final float WIREFRAME_LINE_WIDTH = 2.5F;

  private static final Cache<Parcel, ParcelRenderState> CACHE =
      Caffeine.newBuilder().weakKeys().build();

  void renderGizmos() {
    for (var parcel : GitParcelModClient.PARCELS.values()) {
      var currentState = new ParcelRenderState(parcel);
      var cachedState = CACHE.getIfPresent(parcel);

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
    final io.github.leawind.gitparcel.api.parcel.ParcelTransform transform;

    final List<Gizmo> gizmos = new ArrayList<>(15);

    private ParcelRenderState(Parcel parcel) {
      this(parcel.meta().size(), parcel.meta().anchor(), parcel.visual(), parcel.transform());
    }

    private ParcelRenderState(
        Vec3i size,
        Vec3i anchor,
        Parcel.Visual visual,
        io.github.leawind.gitparcel.api.parcel.ParcelTransform transform) {
      this.size = size;
      this.anchor = anchor;
      this.visual = visual;
      this.transform = transform;
    }

    void generateGizmos() {
      gizmos.clear();

      var pivot = transform.apply(Vec3.ZERO);

      // Wireframe
      if (visual.showWireframe()) {
        var x = new Vec3(size.getX(), 0, 0);
        var y = new Vec3(0, size.getY(), 0);
        var z = new Vec3(0, 0, size.getZ());

        var xyz = new Vec3(size);
        var yz = new Vec3(0, size.getY(), size.getZ());
        var xz = new Vec3(size.getX(), 0, size.getZ());
        var xy = new Vec3(size.getX(), size.getY(), 0);

        x = transform.apply(x);
        y = transform.apply(y);
        z = transform.apply(z);
        xyz = transform.apply(xyz);
        yz = transform.apply(yz);
        xz = transform.apply(xz);
        xy = transform.apply(xy);

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

        var anchorPos = new Vec3(anchor);
        var x = anchorPos.add(new Vec3(ANCHOR_SIZE, 0, 0));
        var y = anchorPos.add(new Vec3(0, ANCHOR_SIZE, 0));
        var z = anchorPos.add(new Vec3(0, 0, ANCHOR_SIZE));

        anchorPos = transform.apply(anchorPos);
        x = transform.apply(x);
        y = transform.apply(y);
        z = transform.apply(z);

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
