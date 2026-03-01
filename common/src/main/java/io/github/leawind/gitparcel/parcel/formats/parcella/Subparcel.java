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

  /**
   * Divide a parcel into subparcels, each subparcel has a size of 16x16x16 blocks.
   *
   * @param parcel Parcel to be subdivided
   * @param anchorPos Absolute position of origin point
   * @return Bounding boxes of subparcels, use absolute coordinates
   */
  public static ArrayList<Subparcel> subdivideParcel(int gridSize, Parcel parcel, Vec3i anchorPos) {
    ArrayList<Subparcel> subparcels = new ArrayList<>(1);

    List<Integer> xDivisions =
        subdivideParcel1D(gridSize, parcel.originX, parcel.sizeX, anchorPos.getX());
    List<Integer> yDivisions =
        subdivideParcel1D(gridSize, parcel.originY, parcel.sizeY, anchorPos.getY());
    List<Integer> zDivisions =
        subdivideParcel1D(gridSize, parcel.originZ, parcel.sizeZ, anchorPos.getZ());

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

  static List<Integer> subdivideParcel1D(int gridSize, int origin, int size, int anchor) {
    List<Integer> divisions = new ArrayList<>(1);

    int current = origin;
    divisions.add(current);
    current = ceilToGrid(gridSize, anchor, current);

    int end = origin + size;
    while (current < end) {
      divisions.add(current);
      current += gridSize;
    }

    divisions.add(end);

    return divisions;
  }

  static int floorToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize);
  }

  static int ceilToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize) + gridSize;
  }
}
