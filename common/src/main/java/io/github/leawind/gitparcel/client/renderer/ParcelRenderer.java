package io.github.leawind.gitparcel.client.renderer;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ParcelRenderer {
  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;
  private static final float LINE_WIDTH = 2.5F;

  void renderGizmos() {
    for (var parcel : GitParcelModClient.PARCELS) {
      var visual = parcel.visual();
      // Wireframe
      if (visual.showWireframe()) {
        Gizmos.cuboid(
            AABB.of(parcel.getBoundingBox()),
            GizmoStyle.stroke(WIREFRAME_COLOR, LINE_WIDTH),
            false);
      }

      // Pivot
      if (visual.showAnchor()) {
        final float pivotSize = 2F;
        final float pivotWidth = LINE_WIDTH;

        var transform = parcel.transform();

        // Pivot and offsets in local space
        Vec3 pivot = Vec3.ZERO;
        Vec3 offsetX = new Vec3(pivotSize, 0, 0);
        Vec3 offsetY = new Vec3(0, pivotSize, 0);
        Vec3 offsetZ = new Vec3(0, 0, pivotSize);

        // Transform to world space
        pivot = transform.apply(pivot);
        offsetX = transform.apply(offsetX);
        offsetY = transform.apply(offsetY);
        offsetZ = transform.apply(offsetZ);

        // Add gizmos
        Gizmos.arrow(pivot, offsetX, 0xFFFF0000, pivotWidth);
        Gizmos.arrow(pivot, offsetY, 0xFF00FF00, pivotWidth);
        Gizmos.arrow(pivot, offsetZ, 0xFF0000FF, pivotWidth);
      }
    }
  }
}
