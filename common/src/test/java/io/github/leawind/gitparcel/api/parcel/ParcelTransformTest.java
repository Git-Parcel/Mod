package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.testutils.AbstractGitParcelTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

public class ParcelTransformTest extends AbstractGitParcelTest {

  @Test
  void testIdentity() {
    ParcelTransform transform = ParcelTransform.IDENTITY;

    assertFalse(transform.hasOrientation());

    for (int i : iter(100)) {
      BlockPos pos = random.nextBlockPos(-100, 100);
      assertEquals(pos, transform.apply(pos));

      Vec3 vec3 = random.nextVec3(-100, 100);
      assertEquals(vec3, transform.applyInverted(vec3));
    }
  }

  @Test
  void testTranslation() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(1, 2, 3));

    assertEquals(new BlockPos(1, 2, 3), transform.apply(BlockPos.ZERO));
    assertEquals(new BlockPos(-1, -2, -3), transform.applyInverted(new BlockPos(0, 0, 0)));
  }

  @Test
  void testMirror() {
    BlockPos pos = new BlockPos(1, 2, 3);

    assertEquals(
        new BlockPos(-2, 2, 3),
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO).apply(pos));
    assertEquals(
        new BlockPos(1, 2, -4),
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, BlockPos.ZERO).apply(pos));
  }

  @Test
  void testRotation() {
    BlockPos pos = new BlockPos(1, 2, 3);

    assertEquals(
        new BlockPos(-4, 2, 1),
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO).apply(pos));
    assertEquals(
        new BlockPos(-2, 2, -4),
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_180, BlockPos.ZERO).apply(pos));
    assertEquals(
        new BlockPos(3, 2, -2),
        new ParcelTransform(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, BlockPos.ZERO).apply(pos));
  }

  @Test
  void testApplyToSize() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);
    Vec3i size = new Vec3i(2, 3, 4);

    // Rotation affects size dimensions
    assertEquals(new Vec3i(4, 3, 2), transform.applyToSize(size));
  }

  @Test
  void testHasOrientation() {
    ParcelTransform transform1 = ParcelTransform.IDENTITY;
    ParcelTransform transform2 =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO);
    ParcelTransform transform3 =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);

    assertFalse(transform1.hasOrientation());
    assertTrue(transform2.hasOrientation());
    assertTrue(transform3.hasOrientation());
  }

  @Test
  void testGetTranslatedOrigin() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(1, 2, 3));
    assertEquals(new BlockPos(1, 2, 3), transform.getTranslatedOrigin());
  }

  @Test
  void testApplyInvertedVec3() {
    var transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(2, 3, 4));
    Vec3 inverted = transform.applyInverted(new Vec3(-3, 7, 1));
    assertEquals(3.0, inverted.x, 1e-6);
    assertEquals(4.0, inverted.y, 1e-6);
    assertEquals(5.0, inverted.z, 1e-6);
  }

  @Test
  void testApplyInvertedBlockPosRoundtrip() {
    for (int i : iter(100)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translate = random.nextVec3i(-100, 100);
      var transform = new ParcelTransform(mirror, rotation, translate);

      for (int j : iter(100)) {
        var pos = random.nextBlockPos(-100, 100);
        assertEquals(pos, transform.applyInverted(transform.apply(pos)));
      }
    }
  }

  @Test
  void testRotateSizeAllRotations() {
    Vec3i size = new Vec3i(3, 5, 7);
    assertEquals(new Vec3i(3, 5, 7), ParcelTransform.rotateSize(Rotation.NONE, size));
    assertEquals(new Vec3i(7, 5, 3), ParcelTransform.rotateSize(Rotation.CLOCKWISE_90, size));
    assertEquals(new Vec3i(3, 5, 7), ParcelTransform.rotateSize(Rotation.CLOCKWISE_180, size));
    assertEquals(
        new Vec3i(7, 5, 3), ParcelTransform.rotateSize(Rotation.COUNTERCLOCKWISE_90, size));
  }

  @Test
  void testApplyToSizeWithMirror() {
    Vec3i size = new Vec3i(3, 5, 7);
    assertEquals(
        size,
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO).applyToSize(size));
    assertEquals(
        size,
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, BlockPos.ZERO).applyToSize(size));
  }

  @Test
  void testApplyToSizeWithTranslation() {
    Vec3i size = new Vec3i(3, 5, 7);
    assertEquals(
        size,
        new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(10, 20, 30))
            .applyToSize(size));
  }

  Vec3i randomApplyToSize(ParcelTransform transform, Vec3i size, int rounds) {
    for (var i : iter(rounds * 2)) {
      size = transform.applyToSize(size);
    }
    return size;
  }

  @Test
  void testToMatrix4fAgainstApply() {
    for (int i : iter(100)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translate = random.nextVec3i(-50, 50);
      var transform = new ParcelTransform(mirror, rotation, translate);

      Matrix4f expected = new Matrix4f();
      transform.apply(expected);
      Matrix4f actual = transform.toMatrix4f();

      assertMatrixEquals(expected, actual, 1e-6f);
    }
  }

  @Test
  void testApplyMatrix4fOnIdentity() {
    var transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(1, 2, 3));
    Matrix4f viaApply = new Matrix4f();
    transform.apply(viaApply);
    Matrix4f viaConstructor = transform.toMatrix4f();

    assertMatrixEquals(viaApply, viaConstructor, 1e-6f);
  }

  @Test
  void testCodecRoundtrip() {
    var transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new Vec3i(1, 2, 3));
    var json = ParcelTransform.CODEC.encodeStart(JsonOps.INSTANCE, transform).getOrThrow();
    var decoded = ParcelTransform.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    assertEquals(transform, decoded);
  }

  @Test
  void testCodecRoundtripIdentity() {
    var json =
        ParcelTransform.CODEC.encodeStart(JsonOps.INSTANCE, ParcelTransform.IDENTITY).getOrThrow();
    var decoded = ParcelTransform.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    assertEquals(ParcelTransform.IDENTITY, decoded);
  }

  @Test
  void testCodecRoundtripAllValues() {
    for (int i : iter(100)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translation = random.nextVec3i(-100, 100);
      var transform = new ParcelTransform(mirror, rotation, translation);

      var json = ParcelTransform.CODEC.encodeStart(JsonOps.INSTANCE, transform).getOrThrow();
      var decoded = ParcelTransform.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
      assertEquals(transform, decoded);
    }
  }

  @Test
  void testIdentityFields() {
    assertEquals(Mirror.NONE, ParcelTransform.IDENTITY.mirror());
    assertEquals(Rotation.NONE, ParcelTransform.IDENTITY.rotation());
    assertEquals(Vec3i.ZERO, ParcelTransform.IDENTITY.translation());
  }

  static void assertMatrixEquals(Matrix4f expected, Matrix4f actual, float epsilon) {
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
