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

  private static Vec3 directionVector(float yaw) {
    double radians = Math.toRadians(yaw);
    return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
  }

  private static float yaw(Vec3 direction) {
    return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
  }
}
