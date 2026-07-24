package io.github.leawind.gitparcel.common.minecraft.logic.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import io.github.leawind.gitparcel.common.utils.TransformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class ParcelTransformMatrixTest extends AbstractMinecraftTest {

  @Test
  void createsTheSameMatrixAsPrimitiveTransformOperations() {
    for (int i = 0; i < 100; i++) {
      var transform =
          new ParcelTransform(
              random.nextEnum(Mirror.class),
              random.nextEnum(Rotation.class),
              random.nextVec3i(-50, 50));
      var expected = new Matrix4f();
      TransformUtils.mirror(transform.mirror(), expected);
      TransformUtils.rotateY(transform.rotation(), expected);
      TransformUtils.translate(transform.translation(), expected);

      assertMatrixEquals(expected, ParcelTransformMatrix.create(transform), 1e-6f);
    }
  }

  @Test
  void appliesIntoAnExistingMatrix() {
    var transform =
        new ParcelTransform(
            Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(1, 2, 3));
    var target = new Matrix4f();

    ParcelTransformMatrix.apply(transform, target);

    assertMatrixEquals(ParcelTransformMatrix.create(transform), target, 1e-6f);
  }

  private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual, float epsilon) {
    assertEquals(expected.m00(), actual.m00(), epsilon);
    assertEquals(expected.m01(), actual.m01(), epsilon);
    assertEquals(expected.m02(), actual.m02(), epsilon);
    assertEquals(expected.m03(), actual.m03(), epsilon);
    assertEquals(expected.m10(), actual.m10(), epsilon);
    assertEquals(expected.m11(), actual.m11(), epsilon);
    assertEquals(expected.m12(), actual.m12(), epsilon);
    assertEquals(expected.m13(), actual.m13(), epsilon);
    assertEquals(expected.m20(), actual.m20(), epsilon);
    assertEquals(expected.m21(), actual.m21(), epsilon);
    assertEquals(expected.m22(), actual.m22(), epsilon);
    assertEquals(expected.m23(), actual.m23(), epsilon);
    assertEquals(expected.m30(), actual.m30(), epsilon);
    assertEquals(expected.m31(), actual.m31(), epsilon);
    assertEquals(expected.m32(), actual.m32(), epsilon);
    assertEquals(expected.m33(), actual.m33(), epsilon);
  }
}
