package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

public class ParcelTransformTest extends AbstractMinecraftTest {

  @Test
  void testIdentity() {
    ParcelTransform transform = ParcelTransform.IDENTITY;

    assertFalse(transform.hasOrientation());

    for (int i = 0; i < 100; i++) {
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
    for (int i = 0; i < 100; i++) {
      var mirror = random.nextEnum(Mirror.class);
      var rotation = random.nextEnum(Rotation.class);
      var translate = random.nextVec3i(-100, 100);
      var transform = new ParcelTransform(mirror, rotation, translate);

      for (int j = 0; j < 100; j++) {
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
    for (int i = 0; i < rounds * 2; i++) {
      size = transform.applyToSize(size);
    }
    return size;
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
    for (int i = 0; i < 100; i++) {
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

}
