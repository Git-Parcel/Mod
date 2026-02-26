package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Vec3i;

public final class Subparcel extends Parcel {

  public Subparcel(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    super(originX, originY, originZ, sizeX, sizeY, sizeZ);
  }

  public Vec3i getCoord(int anchorX, int anchorY, int anchorZ) {
    // TODO test
    return new Vec3i((originX - anchorX) / 16, (originY - anchorY) / 16, (originZ - anchorZ) / 16);
  }

  /**
   * Divide a parcel into subparcels, each subparcel has a size of 16x16x16 blocks.
   *
   * @param parcel Parcel to be subdivided
   * @param anchorPos Absolute position of origin point
   * @return Bounding boxes of subparcels, use absolute coordinates
   */
  public static ArrayList<Subparcel> subdivideParcel(Parcel parcel, Vec3i anchorPos) {
    ArrayList<Subparcel> subparcels = new ArrayList<>(1);

    List<Integer> xDivisions = subdivideParcel1D(parcel.originX, parcel.sizeX, anchorPos.getX());
    List<Integer> yDivisions = subdivideParcel1D(parcel.originY, parcel.sizeY, anchorPos.getY());
    List<Integer> zDivisions = subdivideParcel1D(parcel.originZ, parcel.sizeZ, anchorPos.getZ());

    for (int i = 0; i < xDivisions.size() - 1; i++) {
      int startX = Math.max(xDivisions.get(i), parcel.originX);
      int endX = Math.min(xDivisions.get(i + 1), parcel.getEndX());
      if (startX >= endX) continue;

      for (int j = 0; j < yDivisions.size() - 1; j++) {
        int startY = Math.max(yDivisions.get(j), parcel.originY);
        int endY = Math.min(yDivisions.get(j + 1), parcel.getEndY());
        if (startY >= endY) continue;

        for (int k = 0; k < zDivisions.size() - 1; k++) {
          int startZ = Math.max(zDivisions.get(k), parcel.originZ);
          int endZ = Math.min(zDivisions.get(k + 1), parcel.getEndZ());
          if (startZ >= endZ) continue;

          subparcels.add(
              new Subparcel(startX, startY, startZ, endX - startX, endY - startY, endZ - startZ));
        }
      }
    }

    return subparcels;
  }

  /**
   * Divide a 1D line into subparcels, each subparcel has a size of 16 blocks.
   *
   * @param origin Start position of the line
   * @param size Total length of the line, must be positive
   * @param anchor Anchor position, used to align the subparcels
   * @return List of subparcel edges
   */
  static List<Integer> subdivideParcel1D(int origin, int size, int anchor) {
    List<Integer> divisions = new ArrayList<>(1);

    int current = origin;
    divisions.add(current);
    current = ceilToGrid16(anchor, current);

    int end = origin + size;
    while (current < end) {
      divisions.add(current);
      current += 16;
    }

    divisions.add(end);

    return divisions;
  }

  static int floorToGrid16(int grid, int value) {
    return value - Math.floorMod(value - grid, 16);
  }

  static int ceilToGrid16(int grid, int value) {
    return floorToGrid16(grid, value) + 16;
  }

  public String toString() {
    return String.format(
        "Subparcel[origin=(%d, %d, %d), size=(%d, %d, %d)]",
        originX, originY, originZ, sizeX, sizeY, sizeZ);
  }

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

  public int hashCode() {
    return Objects.hash(originX, originY, originZ, sizeX, sizeY, sizeZ);
  }
}
