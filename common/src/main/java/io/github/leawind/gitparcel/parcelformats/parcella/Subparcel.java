package io.github.leawind.gitparcel.parcelformats.parcella;

import io.github.leawind.gitparcel.api.parcel.Parcel;
import java.util.Objects;
import net.minecraft.core.Vec3i;

public class Subparcel extends Parcel {

  public Subparcel(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    super(originX, originY, originZ, sizeX, sizeY, sizeZ);
  }

  /**
   * todo
   *
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
}
