package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParcelTransformTest {

  RandomForMC random;

  @BeforeEach
  void setup() {
    random = new RandomForMC(12138);
  }

  IntIterable iter(int times) {
    return new IntIterable() {
      @Override
      public @NonNull IntIterator iterator() {
        return new IntIterator() {
          int i = 0;

          @Override
          public int nextInt() {
            return i++;
          }

          @Override
          public boolean hasNext() {
            return i < times;
          }
        };
      }
    };
  }

  @Test
  void testIdentity() {
    ParcelTransform transform = ParcelTransform.IDENTITY;
    for (int i : iter(100)) {
      BlockPos pos = random.nextBlockPos(-100, 100);
      Vec3i vec = random.nextVec3i(-100, 100);
      Vec3 vec3 = random.nextVec3(-100, 100);

      assertEquals(pos, transform.apply(pos));
      assertEquals(vec, transform.apply(vec));
      assertEquals(vec3, transform.applyInverted(vec3));
    }
  }

  @Test
  void testTranslation() {
    ParcelTransform transform = new ParcelTransform(new BlockPos(1, 2, 3));

    assertEquals(new BlockPos(1, 2, 3), transform.apply(BlockPos.ZERO));
    assertEquals(new Vec3i(1, 2, 3), transform.apply(Vec3i.ZERO));
    assertEquals(new BlockPos(-1, -2, -3), transform.applyInverted(new BlockPos(0, 0, 0)));
  }

  @Test
  void testMirrorFrontBack() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(-1, 2, 3), transform.apply(pos));
    assertEquals(new Vec3i(-1, 2, 3), transform.apply(vec));
  }

  @Test
  void testMirrorLeftRight() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(1, 2, -3), transform.apply(pos));
    assertEquals(new Vec3i(1, 2, -3), transform.apply(vec));
  }

  @Test
  void testRotationClockwise90() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(-3, 2, 1), transform.apply(pos));
    assertEquals(new Vec3i(-3, 2, 1), transform.apply(vec));
  }

  @Test
  void testRotationClockwise180() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_180, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(-1, 2, -3), transform.apply(pos));
    assertEquals(new Vec3i(-1, 2, -3), transform.apply(vec));
  }

  @Test
  void testRotationCounterClockwise90() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(3, 2, -1), transform.apply(pos));
    assertEquals(new Vec3i(3, 2, -1), transform.apply(vec));
  }

  @Test
  void testCombinedTransform() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(2, 3, 4));

    BlockPos pos = new BlockPos(3, 4, 5);

    // Apply transformations in order: Mirror -> Rotate -> Translate
    // 1. Mirror: (-3, 4, 5)
    // 2. Rotate 90: (-5, 4, -3)
    // 3. Translate: (-3, 7, 1)
    assertEquals(new BlockPos(-3, 7, 1), transform.apply(pos));
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
    ParcelTransform transform = new ParcelTransform(new BlockPos(1, 2, 3));
    assertEquals(new BlockPos(1, 2, 3), transform.getTranslatedOrigin());
  }

  @Test
  void testApplyInvertedVec3i() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(1, 2, 3));
    Vec3i original = new Vec3i(1, 0, 0);
    Vec3i transformed = transform.apply(original);
    Vec3i inverted = transform.applyInverted(transformed);

    assertEquals(original, inverted);
  }

  @Test
  void testTransformInversion() {
    for (int i : iter(100)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translate = random.nextVec3i(-100, 100);
      var transform = new ParcelTransform(mirror, rotation, translate);

      for (int j : iter(100)) {
        var v = random.nextVec3i(-100, 100);
        assertEquals(v, randomApply(transform, v, 10));

        var s = random.nextVec3i(1, 100);
        assertEquals(s, randomApplyToSize(transform, s, 10));
      }
    }
  }

  @Test
  void testPivotInversion() {
    for (int i : iter(1000)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var worldPivotPos = random.nextVec3i(-100, 100);

      var transform = new ParcelTransform(mirror, rotation, worldPivotPos);
      var ori = transform.applyInverted(worldPivotPos);

      assertEquals(BlockPos.ZERO, ori);
    }
  }

  @Test
  void testTransformBounds() {
    for (int i : iter(100)) {
      var worldCorner1 = random.nextBlockPos(-50, 50);
      var worldCorner2 = random.nextBlockPos(-50, 50);
      var worldBounds = BoundingBox.fromCorners(worldCorner1, worldCorner2);
      var worldSize =
          new Vec3i(worldBounds.getXSpan(), worldBounds.getYSpan(), worldBounds.getZSpan());

      var mirror = random.nextEnum(Mirror.class);
      mirror = Mirror.NONE;
      var rotation = random.nextEnum(Rotation.class);

      var worldPivot = ParcelTransform.getPivotPos(mirror, rotation, worldBounds);
      var transform = new ParcelTransform(mirror, rotation, worldPivot);
      var localSize = transform.applyToSize(worldSize);
      var localPivot = transform.applyInverted(worldPivot);
      var localBounds = BoundingBox.fromCorners(Vec3i.ZERO, localSize);

      assertEquals(Vec3i.ZERO, localPivot);

      for (int x = 0; x < localSize.getX(); x++) {
        for (int y = 0; y < localSize.getY(); y++) {
          for (int z = 0; z < localSize.getZ(); z++) {
            var localPos = new BlockPos(x, y, z);
            assertTrue(localBounds.isInside(localPos));

            var worldPos = transform.apply(localPos);
            assertTrue(worldBounds.isInside(worldPos));
          }
        }
      }
    }
  }

  @Test
  void testApplyInvertedVec3() {
    var transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(2, 3, 4));
    // Forward: mirror(3,4,5)->(-3,4,5), rotate CW90->(-5,4,-3), translate->(-3,7,1)
    assertEquals(new Vec3i(-3, 7, 1), transform.apply(new Vec3i(3, 4, 5)));

    Vec3 inverted = transform.applyInverted(new Vec3(-3, 7, 1));
    assertEquals(3.0, inverted.x, 1e-6);
    assertEquals(4.0, inverted.y, 1e-6);
    assertEquals(5.0, inverted.z, 1e-6);
  }

  @Test
  void testApplyInvertedVec3ViaBlockPos() {
    for (int i : iter(100)) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translate = random.nextVec3i(-100, 100);
      var transform = new ParcelTransform(mirror, rotation, translate);

      for (int j : iter(100)) {
        var pos = random.nextBlockPos(-100, 100);
        var transformedPos = transform.apply(pos);
        var inverted =
            transform.applyInverted(
                new Vec3(transformedPos.getX(), transformedPos.getY(), transformedPos.getZ()));
        assertEquals(pos.getX(), (int) inverted.x);
        assertEquals(pos.getY(), (int) inverted.y);
        assertEquals(pos.getZ(), (int) inverted.z);
      }
    }
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
  void testApplyBoundingBoxTranslationOnly() {
    var transform = new ParcelTransform(new BlockPos(5, 10, 15));
    var box = BoundingBox.fromCorners(new BlockPos(1, 2, 3), new BlockPos(4, 6, 9));
    var result = transform.apply(box);

    assertEquals(6, result.minX());
    assertEquals(12, result.minY());
    assertEquals(18, result.minZ());
    assertEquals(9, result.maxX());
    assertEquals(16, result.maxY());
    assertEquals(24, result.maxZ());
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
    assertEquals(size, new ParcelTransform(new BlockPos(10, 20, 30)).applyToSize(size));
  }

  Vec3i randomApply(ParcelTransform transform, Vec3i v, int rounds) {
    Boolean[] ops = new Boolean[rounds * 2];

    for (int i : iter(rounds)) {
      ops[i * 2] = true;
      ops[i * 2 + 1] = false;
    }
    random.shuffle(ops);
    for (Boolean op : ops) {
      if (op) {
        v = transform.apply(v);
      } else {
        v = transform.applyInverted(v);
      }
    }
    return v;
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
  void testGetPivotPosAllCombinations() {
    var bounds = BoundingBox.fromCorners(new BlockPos(1, 2, 3), new BlockPos(5, 8, 11));

    // NONE × rotations
    assertEquals(
        new BlockPos(1, 2, 3), ParcelTransform.getPivotPos(Mirror.NONE, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(5, 2, 3),
        ParcelTransform.getPivotPos(Mirror.NONE, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(5, 2, 11),
        ParcelTransform.getPivotPos(Mirror.NONE, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(1, 2, 11),
        ParcelTransform.getPivotPos(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, bounds));

    // LEFT_RIGHT × rotations
    assertEquals(
        new BlockPos(1, 2, 11),
        ParcelTransform.getPivotPos(Mirror.LEFT_RIGHT, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(5, 2, -3),
        ParcelTransform.getPivotPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(5, 2, -11),
        ParcelTransform.getPivotPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(1, 2, -11),
        ParcelTransform.getPivotPos(Mirror.LEFT_RIGHT, Rotation.COUNTERCLOCKWISE_90, bounds));

    // FRONT_BACK × rotations
    assertEquals(
        new BlockPos(5, 2, 3),
        ParcelTransform.getPivotPos(Mirror.FRONT_BACK, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(-5, 2, 3),
        ParcelTransform.getPivotPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(-5, 2, 11),
        ParcelTransform.getPivotPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(-1, 2, 11),
        ParcelTransform.getPivotPos(Mirror.FRONT_BACK, Rotation.COUNTERCLOCKWISE_90, bounds));
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

  @Test
  void testTranslationApplyToVec3i() {
    var transform = new ParcelTransform(new Vec3i(10, 20, 30));
    assertEquals(new Vec3i(11, 22, 33), transform.apply(new Vec3i(1, 2, 3)));
    assertEquals(new Vec3i(1, 2, 3), transform.applyInverted(new Vec3i(11, 22, 33)));
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
