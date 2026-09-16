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
  /** The anchor's absolute world position, used as the placement translation. */
  private static final BlockPos ANCHOR_WORLD = new BlockPos(101, 42, -73);

  @Test
  void mapsArchiveOriginToAnchorWorldPosition() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);

        assertEquals(new Vec3(ANCHOR_WORLD), space.toWorld(Vec3.ZERO));
        assertVecEquals(Vec3.ZERO, space.toParcel(new Vec3(ANCHOR_WORLD)));
      }
    }
  }

  @Test
  void roundTripsAnchorRelativePositionsForEveryOrientation() {
    var relativeBlock = new BlockPos(-4, 9, 13);
    var relativePoint = new Vec3(-3.75, 9.5, 13.125);

    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);

        assertEquals(relativeBlock, space.toParcel(space.toWorld(relativeBlock)));
        assertVecEquals(relativePoint, space.toParcel(space.toWorld(relativePoint)));
      }
    }
  }

  @Test
  void vectorsAndDirectionsIgnoreTheTranslation() {
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
    return new ParcelSpace(new ParcelTransform(mirror, rotation, ANCHOR_WORLD));
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
