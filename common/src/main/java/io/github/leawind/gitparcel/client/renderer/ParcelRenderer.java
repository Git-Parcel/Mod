package io.github.leawind.gitparcel.client.renderer;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

public class ParcelRenderer {
  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;
  private static final float LINE_WIDTH = 2.5F;

  protected void render() {
    for (var parcel : GitParcelModClient.PARCELS) {
      if (parcel.visual().showWireframe()) {
        Gizmos.cuboid(
            AABB.of(parcel.boundingBox()), GizmoStyle.stroke(WIREFRAME_COLOR, LINE_WIDTH), false);
        // TODO render more details
      }
    }
  }
}
