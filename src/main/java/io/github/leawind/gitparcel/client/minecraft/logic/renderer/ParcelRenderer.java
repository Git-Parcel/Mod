package io.github.leawind.gitparcel.client.minecraft.logic.renderer;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.minecraft.bridge.GameClientApi;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
/*? if >=26.1 {*/
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
/*?} else {*/
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
 *//*?}*/
import net.minecraft.world.phys.Vec3;

public final class ParcelRenderer {
  ParcelRenderer() {}

  private static final int WIREFRAME_COLOR = 0xFFFFFFFF;

  private static final int X_COLOR = 0xFFFF0000; // red
  private static final int Y_COLOR = 0xFF00FF00; // green
  private static final int Z_COLOR = 0xFF0000FF; // blue

  private static final float WIREFRAME_LINE_WIDTH = 2.5F;

  private static final WeakHashMap<Parcel, ParcelRenderState> CACHE = new WeakHashMap<>();

  void renderGizmos(GameClientApi.Render.Context context) {
    for (var parcel : GitParcelClient.get().getParcels().values()) {
      var currentState = new ParcelRenderState(parcel);
      var cachedState = CACHE.get(parcel);

      if (cachedState == null || !cachedState.equals(currentState)) {
        cachedState = currentState;
        cachedState.generate();
        CACHE.put(parcel, cachedState);
      }

      cachedState.render(context);
    }
  }

  private static final class ParcelRenderState {
    final Vec3i size;
    final Vec3i anchor;
    final Parcel.Visual visual;
    final ParcelTransform transform;

    /*? if >=26.1 {*/
    final List<Gizmo> gizmos = new ArrayList<>(15);
    /*?} else {*/
    /*final List<LineSegment> segments = new ArrayList<>(15);
    *//*?}*/

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

    void render(GameClientApi.Render.Context context) {
      /*? if >=26.1 {*/
      for (var gizmo : gizmos) {
        Gizmos.addGizmo(gizmo);
      }
      /*?} else {*/
      /*drawSegments(context);
      *//*?}*/
    }

    void generate() {
      /*? if >=26.1 {*/
      gizmos.clear();
      /*?} else {*/
      /*segments.clear();
      *//*?}*/

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

        addSegment(pivot, x, X_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(pivot, y, Y_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(pivot, z, Z_COLOR, WIREFRAME_LINE_WIDTH);

        addSegment(yz, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(yz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        addSegment(xz, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(xz, z, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        addSegment(xy, x, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(xy, y, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);

        addSegment(xyz, yz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(xyz, xz, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
        addSegment(xyz, xy, WIREFRAME_COLOR, WIREFRAME_LINE_WIDTH);
      }

      if (visual.showAnchor()) {
        final float ANCHOR_SIZE = 1F;

        var anchorPos =
            new Vec3(
                transform.translation().getX(),
                transform.translation().getY(),
                transform.translation().getZ());
        var x = anchorPos.add(transform.applyVector(new Vec3(ANCHOR_SIZE, 0, 0)));
        var y = anchorPos.add(transform.applyVector(new Vec3(0, ANCHOR_SIZE, 0)));
        var z = anchorPos.add(transform.applyVector(new Vec3(0, 0, ANCHOR_SIZE)));

        addAxis(anchorPos, x, X_COLOR, WIREFRAME_LINE_WIDTH * 2);
        addAxis(anchorPos, y, Y_COLOR, WIREFRAME_LINE_WIDTH * 2);
        addAxis(anchorPos, z, Z_COLOR, WIREFRAME_LINE_WIDTH * 2);
      }
    }

    /*? if >=26.1 {*/
    private void addSegment(Vec3 from, Vec3 to, int colorArgb, float width) {
      gizmos.add(new LineGizmo(from, to, colorArgb, width));
    }

    private void addAxis(Vec3 from, Vec3 to, int colorArgb, float width) {
      gizmos.add(new ArrowGizmo(from, to, colorArgb, width));
    }
    /*?} else {*/
    /*private void addSegment(Vec3 from, Vec3 to, int colorArgb, float width) {
      segments.add(new LineSegment(from, to, colorArgb, width));
    }

    private void addAxis(Vec3 from, Vec3 to, int colorArgb, float width) {
      segments.add(new LineSegment(from, to, colorArgb, width));
    }

    private void drawSegments(GameClientApi.Render.Context context) {
      if (segments.isEmpty()) {
        return;
      }
      var minecraft = Minecraft.getInstance();
      MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
      var consumer = buffers.getBuffer(RenderType.lines());
      PoseStack.Pose pose = context.matrices.last();
      for (LineSegment segment : segments) {
        int r = (segment.colorArgb >> 16) & 0xFF;
        int g = (segment.colorArgb >> 8) & 0xFF;
        int b = segment.colorArgb & 0xFF;
        int a = (segment.colorArgb >> 24) & 0xFF;
        var direction = segment.to().subtract(segment.from());
        var normal = direction.normalize();
        drawLineVertex(
            consumer, pose, segment.from(), r, g, b, a, (float) normal.x, (float) normal.y,
            (float) normal.z);
        drawLineVertex(
            consumer, pose, segment.to(), r, g, b, a, (float) normal.x, (float) normal.y,
            (float) normal.z);
      }
      buffers.endBatch(RenderType.lines());
    }

    private static void drawLineVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 position,
        int r,
        int g,
        int b,
        int a,
        float nx,
        float ny,
        float nz) {
      consumer
          .vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
          .color(r, g, b, a)
          .normal(pose.normal(), nx, ny, nz)
          .endVertex();
    }
    *//*?}*/

    /*? if <26.1 {*/
    /*private record LineSegment(Vec3 from, Vec3 to, int colorArgb, float width) {}
    *//*?}*/

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
