package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.testutils.RandomForMC;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
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
  void testApplyMirror() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(-1, 2, 3), transform.applyMirror(pos));
    assertEquals(new Vec3i(-1, 2, 3), transform.applyMirror(vec));
  }

  @Test
  void testApplyRotation() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);
    Vec3 vec3 = new Vec3(1, 2, 3);

    assertEquals(new BlockPos(-3, 2, 1), transform.applyRotation(pos));
    assertEquals(new Vec3i(-3, 2, 1), transform.applyRotation(vec));
    assertEquals(new Vec3(-3, 2, 1), transform.applyRotation(vec3));
  }

  @Test
  void testApplyTranslation() {
    ParcelTransform transform = new ParcelTransform(new BlockPos(1, 2, 3));
    BlockPos pos = new BlockPos(0, 0, 0);
    Vec3i vec = new Vec3i(0, 0, 0);

    assertEquals(new BlockPos(1, 2, 3), transform.applyTranslation(pos));
    assertEquals(new Vec3i(1, 2, 3), transform.applyTranslation(vec));
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
      var localSize = transform.applyToSizeInverted(worldSize);
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
    Boolean[] ops = new Boolean[rounds * 2];
    for (int i : iter(rounds)) {
      ops[i * 2] = true;
      ops[i * 2 + 1] = false;
    }
    random.shuffle(ops);
    for (Boolean op : ops) {
      if (op) {
        size = transform.applyToSize(size);
      } else {
        size = transform.applyToSizeInverted(size);
      }
    }
    return size;
  }
}
