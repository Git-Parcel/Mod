package io.github.leawind.gitparcel.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.client.GameClientApi;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class ParcelInstanceBoundingBoxRenderer {
  public static final ParcelInstanceBoundingBoxRenderer INSTANCE =
      new ParcelInstanceBoundingBoxRenderer();

  private static final RenderPipeline WIREFRAME_BOX = RenderPipelines.LINES;

  private static final ByteBufferBuilder ALLOCATOR =
      new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

  private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
  private static final Vector3f MODEL_OFFSET = new Vector3f();
  private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

  private static final float LINE_WIDTH = 2.0F;
  private static final float NORMAL_X = 0.0F;
  private static final float NORMAL_Y = 0.0F;
  private static final float NORMAL_Z = 0.0F;

  private MappableRingBuffer vertexBuffer;

  private BufferBuilder buffer;
  private boolean isEmpty = true;

  public void extractAndDrawWaypoint(GameClientApi.Render.Context context) {
    renderWaypoint(context);
    drawWireframeBox(context.minecraft, WIREFRAME_BOX);
  }

  private void renderWaypoint(GameClientApi.Render.Context context) {
    isEmpty = true;
    PoseStack matrices = context.matrices;
    Vec3 camera = context.renderState.cameraRenderState.pos;

    matrices.pushPose();
    matrices.translate(-camera.x, -camera.y, -camera.z);

    if (buffer == null) {
      buffer =
          new BufferBuilder(
              ALLOCATOR, WIREFRAME_BOX.getVertexFormatMode(), WIREFRAME_BOX.getVertexFormat());
    }

    // TODO cache
    GitParcelModClient.PARCEL_INSTANCES.forEach(
        (parcelInstance) -> {
          var box = parcelInstance.boundingBox();
          var aabb = AABB.of(box);

          renderSimpleWireframeBox(
              matrices.last().pose(),
              buffer,
              (float) aabb.minX,
              (float) aabb.minY,
              (float) aabb.minZ,
              (float) aabb.maxX,
              (float) aabb.maxY,
              (float) aabb.maxZ,
              0f,
              1f,
              1f,
              1f);
        });

    matrices.popPose();
  }

  private void renderSimpleWireframeBox(
      Matrix4fc positionMatrix,
      BufferBuilder buffer,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      float r,
      float g,
      float b,
      float alpha) {
    addLine(buffer, positionMatrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, minX, minY, minZ, minX, minY, maxZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, alpha);

    addLine(buffer, positionMatrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, alpha);

    addLine(buffer, positionMatrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, alpha);
    addLine(buffer, positionMatrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, alpha);

    isEmpty = false;
  }

  private void addLine(
      BufferBuilder buffer,
      Matrix4fc positionMatrix,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float r,
      float g,
      float b,
      float alpha) {
    addVertex(buffer, positionMatrix, x1, y1, z1, r, g, b, alpha);
    addVertex(buffer, positionMatrix, x2, y2, z2, r, g, b, alpha);
  }

  private void addVertex(
      BufferBuilder buffer,
      Matrix4fc positionMatrix,
      float x,
      float y,
      float z,
      float r,
      float g,
      float b,
      float alpha) {
    buffer
        .addVertex(positionMatrix, x, y, z)
        .setColor(r, g, b, alpha)
        .setNormal(NORMAL_X, NORMAL_Y, NORMAL_Z)
        .setLineWidth(LINE_WIDTH);
  }

  private void drawWireframeBox(Minecraft client, RenderPipeline pipeline) {

    if (isEmpty) {
      return;
    }

    MeshData builtBuffer = buffer.buildOrThrow();
    MeshData.DrawState drawParameters = builtBuffer.drawState();
    VertexFormat format = drawParameters.format();

    GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
    draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

    vertexBuffer.rotate();
    buffer = null;
  }

  private GpuBuffer upload(
      MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
    int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

    if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
      if (vertexBuffer != null) {
        vertexBuffer.close();
      }

      vertexBuffer =
          new MappableRingBuffer(
              () -> GitParcelMod.MOD_ID + " example render pipeline",
              GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
              vertexBufferSize);
    }

    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

    try (GpuBuffer.MappedView mappedView =
        commandEncoder.mapBuffer(
            vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()),
            false,
            true)) {
      MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
    }

    return vertexBuffer.currentBuffer();
  }

  private static void draw(
      Minecraft client,
      RenderPipeline pipeline,
      MeshData builtBuffer,
      MeshData.DrawState drawParameters,
      GpuBuffer vertices,
      VertexFormat format) {
    GpuBuffer indices;
    VertexFormat.IndexType indexType;

    if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
      builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
      indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
      indexType = builtBuffer.drawState().indexType();
    } else {
      RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
          RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
      indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
      indexType = shapeIndexBuffer.type();
    }

    GpuBufferSlice dynamicTransforms =
        RenderSystem.getDynamicUniforms()
            .writeTransform(
                RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

    try (RenderPass renderPass =
        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                () -> GitParcelMod.MOD_ID + " example render pipeline rendering",
                client.getMainRenderTarget().getColorTextureView(),
                OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(),
                OptionalDouble.empty())) {
      renderPass.setPipeline(pipeline);
      RenderSystem.bindDefaultUniforms(renderPass);
      renderPass.setUniform("DynamicTransforms", dynamicTransforms);
      renderPass.setVertexBuffer(0, vertices);
      renderPass.setIndexBuffer(indices, indexType);
      renderPass.drawIndexed(0, 0, drawParameters.indexCount(), 1);
    }

    builtBuffer.close();
  }

  public void close() {
    ALLOCATOR.close();

    if (vertexBuffer != null) {
      vertexBuffer.close();
      vertexBuffer = null;
    }
  }
}
