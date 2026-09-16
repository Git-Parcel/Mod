package io.github.leawind.gitparcel.common.impl.parcel;

import io.github.leawind.gitparcel.common.api.parcel.content.BlockSectionRegion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * Partitions the anchor-relative content extent of a parcel along a cubic grid whose lines pass
 * through the anchor.
 *
 * <p>Archive coordinates place the anchor at the origin, so the content extent is {@code
 * [-anchor, size - anchor)} and the grid is aligned at multiples of the section size. Adjusting
 * parcel bounds only changes sections on the adjusted boundary; sections keep their grid indices
 * and file names across placements.
 */
public final class BlockSectionPartitioner {
  private BlockSectionPartitioner() {}

  /**
   * Partitions the content extent into grid-aligned sections in anchor-relative coordinates.
   *
   * @param parcelSize The parcel-local size.
   * @param anchor The anchor offset in parcel-local coordinates.
   * @param sectionSize The section edge length.
   */
  public static List<BlockSectionRegion> partition(
      Vec3i parcelSize, Vec3i anchor, int sectionSize) {
    if (sectionSize <= 0) {
      throw new IllegalArgumentException("Section size must be positive");
    }
    List<Integer> xDivisions =
        partitionAxis(sectionSize, -anchor.getX(), parcelSize.getX() - anchor.getX());
    List<Integer> yDivisions =
        partitionAxis(sectionSize, -anchor.getY(), parcelSize.getY() - anchor.getY());
    List<Integer> zDivisions =
        partitionAxis(sectionSize, -anchor.getZ(), parcelSize.getZ() - anchor.getZ());
    var sections = new ArrayList<BlockSectionRegion>();

    for (int x = 0; x < xDivisions.size() - 1; x++) {
      int startX = xDivisions.get(x);
      int endX = xDivisions.get(x + 1);
      if (startX >= endX) continue;
      for (int y = 0; y < yDivisions.size() - 1; y++) {
        int startY = yDivisions.get(y);
        int endY = yDivisions.get(y + 1);
        if (startY >= endY) continue;
        for (int z = 0; z < zDivisions.size() - 1; z++) {
          int startZ = zDivisions.get(z);
          int endZ = zDivisions.get(z + 1);
          if (startZ >= endZ) continue;
          sections.add(
              new BlockSectionRegion(
                  new BlockPos(startX, startY, startZ),
                  new Vec3i(endX - startX, endY - startY, endZ - startZ)));
        }
      }
    }
    return List.copyOf(sections);
  }

  /**
   * Returns the division points of {@code [start, end]} along one axis, with grid lines at
   * multiples of the grid size.
   */
  public static List<Integer> partitionAxis(int gridSize, int start, int end) {
    if (gridSize <= 0) {
      throw new IllegalArgumentException("Grid size must be positive");
    }
    var divisions = new ArrayList<Integer>();
    divisions.add(start);
    int current = ceilToGrid(gridSize, start);
    while (current < end) {
      divisions.add(current);
      current += gridSize;
    }
    divisions.add(end);
    return List.copyOf(divisions);
  }

  /** Returns the largest grid point not greater than {@code value}. */
  public static int floorToGrid(int gridSize, int value) {
    return value - Math.floorMod(value, gridSize);
  }

  /** Returns the smallest grid point strictly greater than {@code value}. */
  public static int ceilToGrid(int gridSize, int value) {
    return value - Math.floorMod(value, gridSize) + gridSize;
  }
}
