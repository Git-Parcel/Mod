package io.github.leawind.gitparcel.common.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ParcelSpaceTest extends AbstractMinecraftTest {
  private static final BlockPos ANCHOR = new BlockPos(7, -3, 11);
  private static final BlockPos TRANSLATION = new BlockPos(101, 42, -73);

  @Test
  void roundTripsAnchorRelativePositionsForEveryOrientation() {
    var localBlock = new BlockPos(-4, 9, 13);
    var localPoint = new Vec3(-3.75, 9.5, 13.125);

    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);

        assertEquals(localBlock, space.toParcel(space.toWorld(localBlock)));
        assertVecEquals(localPoint, space.toParcel(space.toWorld(localPoint)));
      }
    }
  }

  @Test
  void vectorsAndDirectionsIgnoreAnchorAndTranslation() {
    var vector = new Vec3(1.25, -2.5, 3.75);

    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);

        assertVecEquals(vector, space.toParcelVector(space.toWorldVector(vector)));
        for (Direction direction : Direction.values()) {
          assertEquals(
              direction, space.toParcelDirection(space.toWorldDirection(direction)));
        }
      }
    }
  }

  @Test
  void yawRoundTripsForEveryOrientation() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);
        for (float yaw : new float[] {-179.5F, -90F, -17.25F, 0F, 45F, 179.5F}) {
          assertEquals(
              0F,
              wrapDegrees(space.toParcelYaw(space.toWorldYaw(yaw)) - yaw),
              1.0E-4F);
        }
      }
    }
  }

  private static ParcelSpace space(Mirror mirror, Rotation rotation) {
    return new ParcelSpace(new ParcelTransform(mirror, rotation, TRANSLATION), ANCHOR);
  }

  private static void assertVecEquals(Vec3 expected, Vec3 actual) {
    assertEquals(expected.x, actual.x, 1.0E-9);
    assertEquals(expected.y, actual.y, 1.0E-9);
    assertEquals(expected.z, actual.z, 1.0E-9);
  }

  private static float wrapDegrees(float degrees) {
    float wrapped = degrees % 360F;
    if (wrapped >= 180F) {
      wrapped -= 360F;
    }
    if (wrapped < -180F) {
      wrapped += 360F;
    }
    return wrapped;
  }
}
