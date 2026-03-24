package io.github.leawind.gitparcel.client.renderer;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

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
      // TODO render more details

    }
  }
}
