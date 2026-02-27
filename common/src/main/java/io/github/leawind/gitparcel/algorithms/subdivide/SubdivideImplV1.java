package io.github.leawind.gitparcel.algorithms.subdivide;

import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;
import java.util.function.Function;
import net.minecraft.core.BlockPos;

public class SubdivideImplV1 implements SubdivideAlgo {
  public static final SubdivideImplV1 INSTANCE = new SubdivideImplV1();

  @Override
  public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
      Parcel parcel, Function<BlockPos, Integer> values, ResultFactory<T> factory) {

    ArrayList<T> result = new ArrayList<>();

    // Create a 3D array to store the values for easier processing
    int[][][] valueGrid = new int[parcel.sizeX][parcel.sizeY][parcel.sizeZ];
    for (int x = 0; x < parcel.sizeX; x++) {
      for (int y = 0; y < parcel.sizeY; y++) {
        for (int z = 0; z < parcel.sizeZ; z++) {
          BlockPos pos = new BlockPos(parcel.originX + x, parcel.originY + y, parcel.originZ + z);
          valueGrid[x][y][z] = values.apply(pos);
        }
      }
    }

    // Boolean array to track which blocks have been processed
    boolean[][][] visited = new boolean[parcel.sizeX][parcel.sizeY][parcel.sizeZ];

    // Process each block in the parcel
    for (int x = 0; x < parcel.sizeX; x++) {
      for (int y = 0; y < parcel.sizeY; y++) {
        for (int z = 0; z < parcel.sizeZ; z++) {
          if (!visited[x][y][z]) {
            int value = valueGrid[x][y][z];

            // Find the maximum extent in X direction
            int maxX = x;
            while (maxX + 1 < parcel.sizeX
                && !visited[maxX + 1][y][z]
                && valueGrid[maxX + 1][y][z] == value) {
              maxX++;
            }

            // Find the maximum extent in Y direction for the XZ rectangle we've found
            int maxY = y;
            boolean canExtendY = true;
            while (canExtendY && maxY + 1 < parcel.sizeY) {
              for (int curX = x; curX <= maxX; curX++) {
                if (visited[curX][maxY + 1][z] || valueGrid[curX][maxY + 1][z] != value) {
                  canExtendY = false;
                  break;
                }
              }
              if (canExtendY) {
                maxY++;
              }
            }

            // Find the maximum extent in Z direction for the XY rectangle we've found
            int maxZ = z;
            boolean canExtendZ = true;
            while (canExtendZ && maxZ + 1 < parcel.sizeZ) {
              for (int curX = x; curX <= maxX; curX++) {
                for (int curY = y; curY <= maxY; curY++) {
                  if (visited[curX][curY][maxZ + 1] || valueGrid[curX][curY][maxZ + 1] != value) {
                    canExtendZ = false;
                    break;
                  }
                }
                if (!canExtendZ) {
                  break;
                }
              }
              if (canExtendZ) {
                maxZ++;
              }
            }

            // Mark all blocks in this region as visited
            for (int curX = x; curX <= maxX; curX++) {
              for (int curY = y; curY <= maxY; curY++) {
                for (int curZ = z; curZ <= maxZ; curZ++) {
                  visited[curX][curY][curZ] = true;
                }
              }
            }

            // Add the microparcel
            T microparcel =
                factory.create(
                    value,
                    parcel.originX + x,
                    parcel.originY + y,
                    parcel.originZ + z,
                    maxX - x + 1,
                    maxY - y + 1,
                    maxZ - z + 1);
            result.add(microparcel);
          }
        }
      }
    }
    return result;
  }
}
