package io.github.leawind.gitparcel.client.renderer;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

public final class ParcelRenderer {
  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;

  private static final int X_COLOR = 0xFFFF0000; // red
  private static final int Y_COLOR = 0xFF00FF00; // green
  private static final int Z_COLOR = 0xFF0000FF; // blue

  private static final float WIREFRAME_LINE_WIDTH = 2.5F;

  void renderGizmos() {
    for (var parcel : GitParcelModClient.PARCELS) {
      Vec3i size = parcel.meta().size();
      var visual = parcel.visual();
      var transform = parcel.transform();

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

        // transform
        x = transform.apply(x);
        y = transform.apply(y);
        z = transform.apply(z);
        xyz = transform.apply(xyz);
        yz = transform.apply(yz);
        xz = transform.apply(xz);
        xy = transform.apply(xy);

        Gizmos.line(pivot, x, X_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(pivot, y, Y_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(pivot, z, Z_COLOR, WIREFRAME_LINE_WIDTH);

        Gizmos.line(yz, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(yz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        Gizmos.line(xz, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(xz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        Gizmos.line(xy, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(xy, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        Gizmos.line(xyz, yz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(xyz, xz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        Gizmos.line(xyz, xy, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
      }

      if (visual.showAnchor()) {
        final float ANCHOR_SIZE = 1F;

        var anchor = new Vec3(parcel.meta().anchor());
        var x = anchor.add(new Vec3(ANCHOR_SIZE, 0, 0));
        var y = anchor.add(new Vec3(0, ANCHOR_SIZE, 0));
        var z = anchor.add(new Vec3(0, 0, ANCHOR_SIZE));

        anchor = transform.apply(anchor);
        x = transform.apply(x);
        y = transform.apply(y);
        z = transform.apply(z);

        Gizmos.arrow(anchor, x, X_COLOR, WIREFRAME_LINE_WIDTH * 2);
        Gizmos.arrow(anchor, y, Y_COLOR, WIREFRAME_LINE_WIDTH * 2);
        Gizmos.arrow(anchor, z, Z_COLOR, WIREFRAME_LINE_WIDTH * 2);
      }
    }
  }
}
