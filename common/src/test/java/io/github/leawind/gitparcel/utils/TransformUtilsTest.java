package io.github.leawind.gitparcel.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

public class TransformUtilsTest {
  private static final Vec3i V = new Vec3i(2, 3, 5);

  @Test
  void testInvert() {
    assertEquals(Rotation.NONE, TransformUtils.invert(Rotation.NONE));
    assertEquals(Rotation.COUNTERCLOCKWISE_90, TransformUtils.invert(Rotation.CLOCKWISE_90));
    assertEquals(Rotation.CLOCKWISE_180, TransformUtils.invert(Rotation.CLOCKWISE_180));
    assertEquals(Rotation.CLOCKWISE_90, TransformUtils.invert(Rotation.COUNTERCLOCKWISE_90));
  }

  @Test
  void testInvertIsInvolution() {
    for (Rotation r : Rotation.values()) {
      assertEquals(r, TransformUtils.invert(TransformUtils.invert(r)));
    }
  }

  @Test
  void testRotateVec3i() {
    assertEquals(new Vec3i(2, 3, 5), TransformUtils.rotate(Rotation.NONE, V));
    assertEquals(new Vec3i(-5, 3, 2), TransformUtils.rotate(Rotation.CLOCKWISE_90, V));
    assertEquals(new Vec3i(-2, 3, -5), TransformUtils.rotate(Rotation.CLOCKWISE_180, V));
    assertEquals(new Vec3i(5, 3, -2), TransformUtils.rotate(Rotation.COUNTERCLOCKWISE_90, V));
  }

  @Test
  void testRotateVec3iIdentity() {
    for (Rotation r : Rotation.values()) {
      Vec3i rotated = TransformUtils.rotate(r, V);
      Vec3i inverted = TransformUtils.rotateInverted(r, rotated);
      assertEquals(V, inverted);
    }
  }

  @Test
  void testRotateVec3() {
    Vec3 v = new Vec3(2, 3, 5);
    assertEquals(new Vec3(2, 3, 5), TransformUtils.rotate(Rotation.NONE, v));
    assertEquals(new Vec3(-5, 3, 2), TransformUtils.rotate(Rotation.CLOCKWISE_90, v));
    assertEquals(new Vec3(-2, 3, -5), TransformUtils.rotate(Rotation.CLOCKWISE_180, v));
    assertEquals(new Vec3(5, 3, -2), TransformUtils.rotate(Rotation.COUNTERCLOCKWISE_90, v));
  }

  @Test
  void testRotateVec3Identity() {
    Vec3 v = new Vec3(2, 3, 5);
    for (Rotation r : Rotation.values()) {
      Vec3 rotated = TransformUtils.rotate(r, v);
      Vec3 inverted = TransformUtils.rotateInverted(r, rotated);
      assertEquals(v.x, inverted.x, 1e-6);
      assertEquals(v.y, inverted.y, 1e-6);
      assertEquals(v.z, inverted.z, 1e-6);
    }
  }

  @Test
  void testRotateInvertedBlockPos() {
    BlockPos pos = new BlockPos(2, 3, 5);
    for (Rotation r : Rotation.values()) {
      BlockPos rotated = pos.rotate(r);
      BlockPos inverted = TransformUtils.rotateInverted(r, rotated);
      assertEquals(pos, inverted);
    }
  }

  @Test
  void testMirrorVec3() {
    Vec3 v = new Vec3(2, 3, 5);
    assertEquals(new Vec3(2, 3, 5), TransformUtils.mirror(Mirror.NONE, v));
    assertEquals(new Vec3(2, 3, -5), TransformUtils.mirror(Mirror.LEFT_RIGHT, v));
    assertEquals(new Vec3(-2, 3, 5), TransformUtils.mirror(Mirror.FRONT_BACK, v));
  }

  @Test
  void testMirrorVec3i() {
    assertEquals(new Vec3i(2, 3, 5), TransformUtils.mirror(Mirror.NONE, V));
    assertEquals(new Vec3i(2, 3, -5), TransformUtils.mirror(Mirror.LEFT_RIGHT, V));
    assertEquals(new Vec3i(-2, 3, 5), TransformUtils.mirror(Mirror.FRONT_BACK, V));
  }

  @Test
  void testMirrorBlockPos() {
    BlockPos pos = new BlockPos(2, 3, 5);
    assertEquals(new BlockPos(2, 3, 5), TransformUtils.mirror(Mirror.NONE, pos));
    assertEquals(new BlockPos(2, 3, -5), TransformUtils.mirror(Mirror.LEFT_RIGHT, pos));
    assertEquals(new BlockPos(-2, 3, 5), TransformUtils.mirror(Mirror.FRONT_BACK, pos));
  }

  @Test
  void testTranslateVec3i() {
    Vec3i t = new Vec3i(10, 20, 30);
    assertEquals(new Vec3i(12, 23, 35), TransformUtils.translate(t, V));
  }

  @Test
  void testTranslateBlockPos() {
    BlockPos pos = new BlockPos(2, 3, 5);
    Vec3i t = new Vec3i(10, 20, 30);
    assertEquals(new BlockPos(12, 23, 35), TransformUtils.translate(t, pos));
  }

  @Test
  void testTranslateInvertedVec3() {
    Vec3 v = new Vec3(12, 23, 35);
    Vec3i t = new Vec3i(10, 20, 30);
    Vec3 result = TransformUtils.translateInverted(t, v);
    assertEquals(2, result.x, 1e-6);
    assertEquals(3, result.y, 1e-6);
    assertEquals(5, result.z, 1e-6);
  }

  @Test
  void testTranslateInvertedVec3i() {
    Vec3i t = new Vec3i(10, 20, 30);
    assertEquals(V, TransformUtils.translateInverted(t, new Vec3i(12, 23, 35)));
  }

  @Test
  void testTranslateInvertedBlockPos() {
    BlockPos pos = new BlockPos(12, 23, 35);
    Vec3i t = new Vec3i(10, 20, 30);
    assertEquals(new BlockPos(2, 3, 5), TransformUtils.translateInverted(t, pos));
  }

  @Test
  void testTranslateInvertedIsInverse() {
    Vec3i t = new Vec3i(10, 20, 30);
    Vec3i translated = TransformUtils.translate(t, V);
    assertEquals(V, TransformUtils.translateInverted(t, translated));
  }

  @Test
  void testRotateYMatrix4F() {
    Matrix4f matrix = new Matrix4f();
    TransformUtils.rotateY(Rotation.CLOCKWISE_90, matrix);
    Vector4f v = new Vector4f(2, 3, 5, 1);
    matrix.transform(v);
    assertEquals(-5, v.x, 1e-5);
    assertEquals(3, v.y, 1e-5);
    assertEquals(2, v.z, 1e-5);
  }

  @Test
  void testMirrorMatrix4fLeftRight() {
    Matrix4f matrix = new Matrix4f();
    TransformUtils.mirror(Mirror.LEFT_RIGHT, matrix);
    Vector4f v = new Vector4f(2, 3, 5, 1);
    matrix.transform(v);
    assertEquals(2, v.x, 1e-5);
    assertEquals(3, v.y, 1e-5);
    assertEquals(-5, v.z, 1e-5);
  }

  @Test
  void testMirrorMatrix4fFrontBack() {
    Matrix4f matrix = new Matrix4f();
    TransformUtils.mirror(Mirror.FRONT_BACK, matrix);
    Vector4f v = new Vector4f(2, 3, 5, 1);
    matrix.transform(v);
    assertEquals(-2, v.x, 1e-5);
    assertEquals(3, v.y, 1e-5);
    assertEquals(5, v.z, 1e-5);
  }

  @Test
  void testTranslateMatrix4f() {
    Matrix4f matrix = new Matrix4f();
    Vec3i t = new Vec3i(10, 20, 30);
    TransformUtils.translate(t, matrix);
    Vector3f v = new Vector3f(2, 3, 5);
    matrix.transformPosition(v);
    assertEquals(12, v.x, 1e-5);
    assertEquals(23, v.y, 1e-5);
    assertEquals(35, v.z, 1e-5);
  }

  @Test
  void testMirrorIsInvolution() {
    for (Mirror m : Mirror.values()) {
      Vec3 v = new Vec3(2, 3, 5);
      Vec3 mirrored = TransformUtils.mirror(m, v);
      Vec3 mirroredAgain = TransformUtils.mirror(m, mirrored);
      assertEquals(v.x, mirroredAgain.x, 1e-6);
      assertEquals(v.y, mirroredAgain.y, 1e-6);
      assertEquals(v.z, mirroredAgain.z, 1e-6);
    }
  }
}
