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

  /** Rule 3.1: the step transform is an involution pair across every facing and orientation. */
  @Test
  void roundTripsRotationStepsForEveryFacingAndOrientation() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = space(mirror, rotation);
        for (Direction facing : Direction.values()) {
          for (int step = 0; step < 8; step++) {
            int world = space.toWorldRotationStep(facing, step);
            assertEquals(
                step,
                space.toParcelRotationStep(facing, world),
                "step round trip for mirror=%s rotation=%s facing=%s".formatted(mirror, rotation, facing));
          }
        }
      }
    }
  }

  /** Wall-mounted frames stay upright when the world rotates around Y. */
  @Test
  void wallFramesKeepTheirStepUnderRotation() {
    var space = space(Mirror.NONE, Rotation.CLOCKWISE_90);
    assertEquals(3, space.toWorldRotationStep(Direction.SOUTH, 3));
    assertEquals(7, space.toWorldRotationStep(Direction.WEST, 7));
  }

  /** Mirroring flips the in-plane handedness, negating the step (mod 8). */
  @Test
  void wallFramesNegateTheirStepUnderMirror() {
    var space = space(Mirror.LEFT_RIGHT, Rotation.NONE);
    assertEquals(5, space.toWorldRotationStep(Direction.SOUTH, 3));
    assertEquals(0, space.toWorldRotationStep(Direction.EAST, 0));
  }

  /** Floor frames rotate with the world because their reference top is a horizontal direction. */
  @Test
  void floorFramesStepUnderRotation() {
    var space = space(Mirror.NONE, Rotation.CLOCKWISE_90);
    assertEquals(5, space.toWorldRotationStep(Direction.UP, 3));
    assertEquals(5, space.toWorldRotationStep(Direction.DOWN, 7));
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
