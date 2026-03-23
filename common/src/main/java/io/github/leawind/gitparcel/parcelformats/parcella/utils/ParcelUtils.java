package io.github.leawind.gitparcel.parcelformats.parcella.utils;

import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Vec3i;

/** Utility class for parcel-related operations, such as subdividing parcels into subparcels. */
public class ParcelUtils {

  public static ArrayList<Subparcel> subdivideParcel(Vec3i size, Vec3i anchorPos, int gridSize) {
    ArrayList<Subparcel> subparcels = new ArrayList<>(1);

    int sizeX = size.getX();
    int sizeY = size.getY();
    int sizeZ = size.getZ();

    List<Integer> xDivisions = subdivideParcel1D(gridSize, sizeX, anchorPos.getX());
    List<Integer> yDivisions = subdivideParcel1D(gridSize, sizeY, anchorPos.getY());
    List<Integer> zDivisions = subdivideParcel1D(gridSize, sizeZ, anchorPos.getZ());

    for (int i = 0; i < xDivisions.size() - 1; i++) {
      int startX = Math.max(xDivisions.get(i), 0);
      int endX = Math.min(xDivisions.get(i + 1), sizeX);
      if (startX >= endX) continue;

      for (int j = 0; j < yDivisions.size() - 1; j++) {
        int startY = Math.max(yDivisions.get(j), 0);
        int endY = Math.min(yDivisions.get(j + 1), sizeY);
        if (startY >= endY) continue;

        for (int k = 0; k < zDivisions.size() - 1; k++) {
          int startZ = Math.max(zDivisions.get(k), 0);
          int endZ = Math.min(zDivisions.get(k + 1), sizeZ);
          if (startZ >= endZ) continue;

          subparcels.add(
              new Subparcel(startX, startY, startZ, endX - startX, endY - startY, endZ - startZ));
        }
      }
    }

    return subparcels;
  }

  /**
   * Subdivide a 1D segment into grid-aligned divisions.
   *
   * @param gridSize Size of a grid
   * @param end End of the segment
   * @param anchor Anchor position
   * @return List of division points
   */
  public static List<Integer> subdivideParcel1D(int gridSize, int end, int anchor) {
    List<Integer> divisions = new ArrayList<>(1);

    int current = 0;

    divisions.add(current);
    current = ceilToGrid(gridSize, anchor, current);

    while (current < end) {
      divisions.add(current);
      current += gridSize;
    }

    divisions.add(end);

    return divisions;
  }

  /**
   * Floor a value to the nearest grid line.
   *
   * @param gridSize Size of a grid
   * @param gridOffset Grid offset
   * @param value Value to floor
   * @return Floored value
   */
  public static int floorToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize);
  }

  /**
   * Ceil a value to the nearest grid line.
   *
   * @param gridSize Size of a grid
   * @param gridOffset Grid offset
   * @param value Value to ceil
   * @return Ceiled value
   */
  public static int ceilToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize) + gridSize;
  }
}
