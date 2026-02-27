package io.github.leawind.gitparcel.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Represents an axis-aligned rectangular region in the world defined by an origin (minimum corner)
 * and a size along each axis.
 *
 * <p>The origin ({@code originX}, {@code originY}, {@code originZ}) corresponds to the minimum
 * inclusive block position of the region. The size ({@code sizeX}, {@code sizeY}, {@code sizeZ})
 * represents the extent along each axis in blocks.
 *
 * <p>The maximum block position is inclusive and equals {@code (origin + size - 1)} on each axis.
 * The end position is exclusive and equals {@code (origin + size)} on each axis.
 *
 * <p>This class is immutable.
 */
public class Parcel {

  /** Minimum X coordinate (inclusive). */
  public final int originX;

  /** Minimum Y coordinate (inclusive). */
  public final int originY;

  /** Minimum Z coordinate (inclusive). */
  public final int originZ;

  /** Size along the X axis in blocks. */
  public final int sizeX;

  /** Size along the Y axis in blocks. */
  public final int sizeY;

  /** Size along the Z axis in blocks. */
  public final int sizeZ;

  /**
   * Creates a parcel from a {@link BoundingBox}.
   *
   * <p>The bounding box minimum corner is used as the origin. The spans along each axis are used as
   * the size.
   *
   * @param boundingBox the bounding box defining the parcel
   */
  public Parcel(BoundingBox boundingBox) {
    this(
        boundingBox.minX(),
        boundingBox.minY(),
        boundingBox.minZ(),
        boundingBox.getXSpan(),
        boundingBox.getYSpan(),
        boundingBox.getZSpan());
  }

  /**
   * Creates a parcel from an origin position and a size vector.
   *
   * @param origin the minimum corner (inclusive)
   * @param size the size along each axis in blocks
   */
  public Parcel(BlockPos origin, Vec3i size) {
    this(origin.getX(), origin.getY(), origin.getZ(), size.getX(), size.getY(), size.getZ());
  }

  /**
   * Creates a parcel from raw coordinates and sizes.
   *
   * @param originX minimum X coordinate (inclusive)
   * @param originY minimum Y coordinate (inclusive)
   * @param originZ minimum Z coordinate (inclusive)
   * @param sizeX size along X in blocks
   * @param sizeY size along Y in blocks
   * @param sizeZ size along Z in blocks
   */
  public Parcel(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    this.originX = originX;
    this.originY = originY;
    this.originZ = originZ;
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.sizeZ = sizeZ;
  }

  /**
   * Returns the origin (minimum inclusive corner) as a {@link BlockPos}.
   *
   * @return the origin position
   */
  public BlockPos getOrigin() {
    return new BlockPos(originX, originY, originZ);
  }

  /**
   * Returns the size of this parcel as a {@link Vec3i}.
   *
   * @return the size vector
   */
  public Vec3i getSize() {
    return new Vec3i(sizeX, sizeY, sizeZ);
  }

  /**
   * Returns the maximum inclusive block position of this parcel.
   *
   * @return the maximum inclusive position
   */
  public BlockPos getMaxPos() {
    return new BlockPos(originX + sizeX - 1, originY + sizeY - 1, originZ + sizeZ - 1);
  }

  /**
   * Returns the exclusive end position of this parcel.
   *
   * <p>The returned coordinates equal {@code origin + size} on each axis.
   *
   * @return the exclusive end position
   */
  public Vec3i getEnd() {
    return new Vec3i(originX + sizeX, originY + sizeY, originZ + sizeZ);
  }

  /**
   * Returns the maximum inclusive X coordinate.
   *
   * @return max X (inclusive)
   */
  public int getMaxX() {
    return originX + sizeX - 1;
  }

  /**
   * Returns the maximum inclusive Y coordinate.
   *
   * @return max Y (inclusive)
   */
  public int getMaxY() {
    return originY + sizeY - 1;
  }

  /**
   * Returns the maximum inclusive Z coordinate.
   *
   * @return max Z (inclusive)
   */
  public int getMaxZ() {
    return originZ + sizeZ - 1;
  }

  /**
   * Returns the exclusive end X coordinate.
   *
   * @return end X (exclusive)
   */
  public int getEndX() {
    return originX + sizeX;
  }

  /**
   * Returns the exclusive end Y coordinate.
   *
   * @return end Y (exclusive)
   */
  public int getEndY() {
    return originY + sizeY;
  }

  /**
   * Returns the exclusive end Z coordinate.
   *
   * @return end Z (exclusive)
   */
  public int getEndZ() {
    return originZ + sizeZ;
  }

  /**
   * Returns the total number of blocks contained in this parcel.
   *
   * @return the volume in blocks
   */
  public int getVolume() {
    return sizeX * sizeY * sizeZ;
  }

  /**
   * Converts this parcel to a {@link BoundingBox}.
   *
   * <p>The resulting bounding box uses inclusive minimum and maximum coordinates.
   *
   * @return a bounding box representing this parcel
   */
  public BoundingBox getBoundingBox() {
    return new BoundingBox(originX, originY, originZ, getMaxX(), getMaxY(), getMaxZ());
  }

  /**
   * Converts this parcel to an {@link AABB}.
   *
   * <p>The resulting AABB uses inclusive minimum coordinates and exclusive maximum coordinates.
   *
   * @return an axis-aligned bounding box representing this parcel
   */
  public AABB getAABB() {
    return new AABB(originX, originY, originZ, getEndX(), getEndY(), getEndZ());
  }

  @Override
  public String toString() {
    return String.format(
        "Parcel:%dx%dx%d@(%d,%d,%d)", sizeX, sizeY, sizeZ, originX, originY, originZ);
  }

  /**
   * Returns whether this parcel has the same size as the given vector.
   *
   * @param size the size vector to compare against
   * @return true if the sizes are equal, false otherwise
   */
  public boolean sizeEquals(Vec3i size) {
    return sizeX == size.getX() && sizeY == size.getY() && sizeZ == size.getZ();
  }

  /**
   * Creates a parcel from two corner positions.
   *
   * <p>The resulting parcel spans the axis-aligned region defined by the two corners.
   *
   * @param corner1 first corner
   * @param corner2 second corner
   * @return a parcel covering the region between the two corners
   */
  public static Parcel fromCorners(BlockPos corner1, BlockPos corner2) {
    return new Parcel(BoundingBox.fromCorners(corner1, corner2));
  }
}
