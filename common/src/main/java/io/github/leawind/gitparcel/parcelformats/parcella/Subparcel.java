package io.github.leawind.gitparcel.parcelformats.parcella;

import java.util.Objects;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class Subparcel {

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

  public Subparcel(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    this.originX = originX;
    this.originY = originY;
    this.originZ = originZ;
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.sizeZ = sizeZ;
  }

  /**
   * @return Coordinate of this subparcel
   */
  public Vec3i getCoord(int gridSize, Vec3i anchorPos) {
    return getCoord(gridSize, anchorPos.getX(), anchorPos.getY(), anchorPos.getZ());
  }

  /**
   * Get the coordinate of this subparcel based on the anchor position.
   *
   * @param gridSize Size of a grid
   * @param anchorX Absolute X coordinate of the anchor position
   * @param anchorY Absolute Y coordinate of the anchor position
   * @param anchorZ Absolute Z coordinate of the anchor position
   */
  public Vec3i getCoord(int gridSize, int anchorX, int anchorY, int anchorZ) {
    return new Vec3i(
        Math.floorDiv(originX - anchorX, gridSize),
        Math.floorDiv(originY - anchorY, gridSize),
        Math.floorDiv(originZ - anchorZ, gridSize));
  }

  @Override
  public String toString() {
    return String.format(
        "Subparcel[origin=(%d, %d, %d), size=(%d, %d, %d)]",
        originX, originY, originZ, sizeX, sizeY, sizeZ);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Subparcel sp) {
      return this.originX == sp.originX
          && this.originY == sp.originY
          && this.originZ == sp.originZ
          && this.sizeX == sp.sizeX
          && this.sizeY == sp.sizeY
          && this.sizeZ == sp.sizeZ;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(originX, originY, originZ, sizeX, sizeY, sizeZ);
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
   * Converts this parcel to a {@link BoundingBox}.
   *
   * <p>The resulting bounding box uses inclusive minimum and maximum coordinates.
   *
   * @return a bounding box representing this parcel
   */
  public BoundingBox getBoundingBox() {
    return new BoundingBox(originX, originY, originZ, getMaxX(), getMaxY(), getMaxZ());
  }
}
