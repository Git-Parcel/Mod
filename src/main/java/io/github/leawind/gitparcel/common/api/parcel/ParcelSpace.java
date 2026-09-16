package io.github.leawind.gitparcel.common.api.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Converts points between world space and the anchor-relative parcel space of one placement.
 *
 * <p>The transform's translation is the anchor's absolute world position, and its mirror and
 * rotation describe the placement orientation. Archive content is always interpreted in
 * anchor-relative coordinates with canonical orientation, so {@link #toWorld} and {@link #toParcel}
 * are the single definition point of the frame change (SEMANTICS.md rule 3.3).
 */
public record ParcelSpace(ParcelTransform transform) {
  public Vec3 toWorld(Vec3 relative) {
    return transform.apply(relative);
  }

  public BlockPos toWorld(BlockPos relative) {
    return transform.apply(relative);
  }

  public Vec3 toParcel(Vec3 world) {
    return transform.applyInverted(world);
  }

  public BlockPos toParcel(BlockPos world) {
    return transform.applyInverted(world);
  }

  public Vec3 toWorldVector(Vec3 vector) {
    return transform.applyVector(vector);
  }

  public Vec3 toParcelVector(Vec3 vector) {
    return transform.applyVectorInverted(vector);
  }

  public Direction toWorldDirection(Direction direction) {
    Vec3 vector = toWorldVector(Vec3.atLowerCornerOf(direction.getUnitVec3i()));
    return Direction.getNearest(
        (int) Math.round(vector.x), (int) Math.round(vector.y), (int) Math.round(vector.z), direction);
  }

  public Direction toParcelDirection(Direction direction) {
    Vec3 vector = toParcelVector(Vec3.atLowerCornerOf(direction.getUnitVec3i()));
    return Direction.getNearest(
        (int) Math.round(vector.x), (int) Math.round(vector.y), (int) Math.round(vector.z), direction);
  }

  public float toWorldYaw(float yaw) {
    return yaw(toWorldVector(directionVector(yaw)));
  }

  public float toParcelYaw(float yaw) {
    return yaw(toParcelVector(directionVector(yaw)));
  }

  /**
   * Transforms an item-frame style rotation step (a 45° in-plane angle) of a frame with the given
   * parcel-space facing into world space.
   *
   * <p>The step is interpreted as a vector: the item's top direction starts at a per-facing
   * reference (world up for wall frames, a fixed horizontal direction for floor and ceiling frames
   * per the vanilla rendering convention) and rotates around the frame's facing axis by −45° per
   * step. The top transforms as a vector and the facing as an orientation, and the transformed step
   * is the quantized angle back from the transformed reference.
   */
  public int toWorldRotationStep(Direction parcelFacing, int step) {
    Vec3 top = transform.applyVector(itemTop(parcelFacing, step));
    return stepFromTop(toWorldDirection(parcelFacing), top);
  }

  /** Inverse of {@link #toWorldRotationStep}. */
  public int toParcelRotationStep(Direction worldFacing, int step) {
    Vec3 top = transform.applyVectorInverted(itemTop(worldFacing, step));
    return stepFromTop(toParcelDirection(worldFacing), top);
  }

  /** The item's top direction for one frame facing and rotation step. */
  private static Vec3 itemTop(Direction facing, int step) {
    return rotateAround(facing.getUnitVec3(), -45.0 * step, referenceTop(facing));
  }

  /**
   * The item's top direction at step zero: world up on walls; for floor and ceiling frames a fixed
   * horizontal direction following the vanilla upright rendering convention.
   */
  private static Vec3 referenceTop(Direction facing) {
    return switch (facing) {
      case DOWN -> new Vec3(0, 0, 1);
      case UP -> new Vec3(-1, 0, 0);
      default -> new Vec3(0, 1, 0);
    };
  }

  /** The 45°-quantized step whose item top matches {@code top}; nearest step on drift. */
  private static int stepFromTop(Direction facing, Vec3 top) {
    Vec3 reference = referenceTop(facing);
    Vec3 axis = facing.getUnitVec3();
    int best = 0;
    double bestDistance = Double.MAX_VALUE;
    for (int candidate = 0; candidate < 8; candidate++) {
      double distance = rotateAround(axis, -45.0 * candidate, reference).subtract(top).lengthSqr();
      if (distance < bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }
    return best;
  }

  /** Rodrigues rotation of {@code vector} around the unit {@code axis}. */
  private static Vec3 rotateAround(Vec3 axis, double degrees, Vec3 vector) {
    double radians = Math.toRadians(degrees);
    double cos = Math.cos(radians);
    double sin = Math.sin(radians);
    double x = axis.x;
    double y = axis.y;
    double z = axis.z;
    double crossX = y * vector.z - z * vector.y;
    double crossY = z * vector.x - x * vector.z;
    double crossZ = x * vector.y - y * vector.x;
    double dot = x * vector.x + y * vector.y + z * vector.z;
    return new Vec3(
        vector.x * cos + crossX * sin + x * dot * (1 - cos),
        vector.y * cos + crossY * sin + y * dot * (1 - cos),
        vector.z * cos + crossZ * sin + z * dot * (1 - cos));
  }

  private static Vec3 directionVector(float yaw) {
    double radians = Math.toRadians(yaw);
    return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
  }

  private static float yaw(Vec3 direction) {
    return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
  }
}
