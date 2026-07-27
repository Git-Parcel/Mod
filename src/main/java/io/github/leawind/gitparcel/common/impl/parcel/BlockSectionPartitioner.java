package io.github.leawind.gitparcel.common.impl.parcel;

import io.github.leawind.gitparcel.common.api.parcel.content.BlockSectionRegion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/** Partitions parcel-space block bounds along a world-aligned cubic grid. */
public final class BlockSectionPartitioner {
  private BlockSectionPartitioner() {}

  public static List<BlockSectionRegion> partition(
      Vec3i parcelSize, Vec3i anchor, int sectionSize) {
    if (sectionSize <= 0) {
      throw new IllegalArgumentException("Section size must be positive");
    }
    List<Integer> xDivisions = partitionAxis(sectionSize, parcelSize.getX(), anchor.getX());
    List<Integer> yDivisions = partitionAxis(sectionSize, parcelSize.getY(), anchor.getY());
    List<Integer> zDivisions = partitionAxis(sectionSize, parcelSize.getZ(), anchor.getZ());
    var sections = new ArrayList<BlockSectionRegion>();

    for (int x = 0; x < xDivisions.size() - 1; x++) {
      int startX = Math.max(xDivisions.get(x), 0);
      int endX = Math.min(xDivisions.get(x + 1), parcelSize.getX());
      if (startX >= endX) continue;
      for (int y = 0; y < yDivisions.size() - 1; y++) {
        int startY = Math.max(yDivisions.get(y), 0);
        int endY = Math.min(yDivisions.get(y + 1), parcelSize.getY());
        if (startY >= endY) continue;
        for (int z = 0; z < zDivisions.size() - 1; z++) {
          int startZ = Math.max(zDivisions.get(z), 0);
          int endZ = Math.min(zDivisions.get(z + 1), parcelSize.getZ());
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

  public static List<Integer> partitionAxis(int gridSize, int end, int anchor) {
    if (gridSize <= 0) {
      throw new IllegalArgumentException("Grid size must be positive");
    }
    var divisions = new ArrayList<Integer>();
    divisions.add(0);
    int current = ceilToGrid(gridSize, anchor, 0);
    while (current < end) {
      divisions.add(current);
      current += gridSize;
    }
    divisions.add(end);
    return List.copyOf(divisions);
  }

  public static int floorToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize);
  }

  public static int ceilToGrid(int gridSize, int gridOffset, int value) {
    return value - Math.floorMod(value - gridOffset, gridSize) + gridSize;
  }
}
