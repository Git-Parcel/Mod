package io.github.leawind.gitparcel.common.api.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * The anchor-relative content extent of a parcel: the half-open box {@code [-anchor, size -
 * anchor)} around the parcel-space origin.
 *
 * <p>Spatial edges inside this extent are inside-pointing (SEMANTICS.md definition 3.2): they
 * travel as parcel-relative values and transform with the placement. Values outside it are
 * outside-pointing world references and stay identical on both sides.
 */
public record ParcelExtent(Vec3i size, Vec3i anchor) {
  public ParcelExtent {
    if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
      throw new IllegalArgumentException("Parcel extent size must be positive: " + size);
    }
  }

  /** The smallest corner of the extent in parcel space. */
  public Vec3i minCorner() {
    return new Vec3i(-anchor.getX(), -anchor.getY(), -anchor.getZ());
  }

  /** The corner just outside the extent in parcel space. */
  public Vec3i maxCorner() {
    return new Vec3i(
        size.getX() - anchor.getX(),
        size.getY() - anchor.getY(),
        size.getZ() - anchor.getZ());
  }

  public boolean contains(double x, double y, double z) {
    return x >= minCorner().getX()
        && y >= minCorner().getY()
        && z >= minCorner().getZ()
        && x < maxCorner().getX()
        && y < maxCorner().getY()
        && z < maxCorner().getZ();
  }

  public boolean contains(Vec3 point) {
    return contains(point.x, point.y, point.z);
  }

  public boolean contains(BlockPos pos) {
    return contains(pos.getX(), pos.getY(), pos.getZ());
  }
}
