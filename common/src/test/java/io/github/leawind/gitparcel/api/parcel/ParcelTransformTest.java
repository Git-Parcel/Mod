package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

public class ParcelTransformTest {
  @Test
  void testNoneTransform() {
    ParcelTransform transform = ParcelTransform.none();

    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);
    Vec3 vec3 = new Vec3(1, 2, 3);

    assertEquals(pos, transform.apply(pos));
    assertEquals(vec, transform.apply(vec));
    assertEquals(vec3, transform.applyInverted(vec3));
  }

  @Test
  void testTranslation() {
    ParcelTransform transform = new ParcelTransform(new BlockPos(1, 2, 3));
    BlockPos pos = new BlockPos(0, 0, 0);
    Vec3i vec = new Vec3i(0, 0, 0);

    assertEquals(new BlockPos(1, 2, 3), transform.apply(pos));
    assertEquals(new Vec3i(1, 2, 3), transform.apply(vec));
    assertEquals(new BlockPos(-1, -2, -3), transform.applyInverted(new BlockPos(0, 0, 0)));
  }

  @Test
  void testMirrorLeftRight() {
    ParcelTransform transform = new ParcelTransform(Mirror.NONE, Rotation.NONE, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(1, 2, 3), transform.apply(pos));
    assertEquals(new Vec3i(1, 2, 3), transform.apply(vec));
  }

  @Test
  void testMirrorFrontBack() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.FRONT_BACK, Rotation.NONE, BlockPos.ZERO);
    BlockPos pos = new BlockPos(1, 2, 3);
    Vec3i vec = new Vec3i(1, 2, 3);

    assertEquals(new BlockPos(3, 2, -1), transform.apply(pos));
    assertEquals(new Vec3i(3, 2, -1), transform.apply(vec));
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
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, new BlockPos(1, 2, 3));
    BlockPos pos = new BlockPos(1, 0, 0);

    // Apply transformations in order: Mirror -> Rotate -> Translate
    // 1. Mirror: (-1, 0, 0)
    // 2. Rotate 90: (0, 0, -1)
    // 3. Translate: (1, 2, 2)
    assertEquals(new BlockPos(1, 2, 2), transform.apply(pos));
  }

  @Test
  void testInvertedTransform() {
    ParcelTransform transform =
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, new BlockPos(1, 2, 3));
    BlockPos original = new BlockPos(1, 0, 0);
    BlockPos transformed = transform.apply(original);
    BlockPos inverted = transform.applyInverted(transformed);

    assertEquals(original, inverted);
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
  void testIsMirroredOrRotated() {
    ParcelTransform transform1 = ParcelTransform.none();
    ParcelTransform transform2 =
        new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, BlockPos.ZERO);
    ParcelTransform transform3 =
        new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, BlockPos.ZERO);

    assertFalse(transform1.isMirroredOrRotated());
    assertTrue(transform2.isMirroredOrRotated());
    assertTrue(transform3.isMirroredOrRotated());
  }
}
