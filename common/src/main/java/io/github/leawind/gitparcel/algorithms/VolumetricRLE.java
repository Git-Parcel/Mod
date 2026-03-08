package io.github.leawind.gitparcel.algorithms;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface VolumetricRLE {

  List<Run> encode(int sizeX, int sizeY, int sizeZ, ValueGetter values);

  /**
   * Represents a run of blocks with the same value.
   *
   * <ul>
   *   <li>(minX, minY, minZ) is the inclusive minimum position of the run.
   *   <li>(endX, endY, endZ) is the exclusive maximum position of the run.
   * </ul>
   */
  record Run(int value, int minX, int minY, int minZ, int endX, int endY, int endZ) {
    int maxX() {
      return endX - 1;
    }

    int maxY() {
      return endY - 1;
    }

    int maxZ() {
      return endZ - 1;
    }
  }

  interface ValueGetter {
    /**
     * Get the value at the given position.
     *
     * <p>The (x, y, z) position is relative to the origin of the untransformed subparcel.
     *
     * @return The value at the given position.
     */
    int get(int x, int y, int z);
  }

  interface ResultFactory<T> {
    T create(int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ);
  }

  VolumetricRLE IMPL =
      new VolumetricRLE() {

        @Override
        public List<Run> encode(int sizeX, int sizeY, int sizeZ, ValueGetter values) {

          final int sizeXZ = sizeX * sizeZ;
          final int totalSize = sizeY * sizeXZ;

          ArrayList<Run> result = new ArrayList<>(totalSize / 2 + 1);

          final int[] valueGrid = new int[totalSize];
          final boolean[] visited = new boolean[totalSize];
          for (int y = 0; y < sizeY; y++) {
            final int yOffset = y * sizeXZ;
            for (int x = 0; x < sizeX; x++) {
              final int xOffset = yOffset + x * sizeZ;
              for (int z = 0; z < sizeZ; z++) {
                // idx = xOffset + z;
                valueGrid[xOffset + z] = values.get(x, y, z);
              }
            }
          }

          int[] groupIndices = new int[totalSize];
          int groupIndicesSize = 0;

          for (int y = 0; y < sizeY; y++) {
            final int yOffset = y * sizeXZ;

            for (int x = 0; x < sizeX; x++) {
              final int xSizeZ = x * sizeZ;
              final int xOffset = yOffset + xSizeZ;

              for (int z = 0; z < sizeZ; z++) {
                final int idx = xOffset + z;

                if (visited[idx]) continue;
                groupIndicesSize = 0;

                final int value = valueGrid[idx];

                int boundY = y + 1;
                int boundX = x + 1;
                int boundZ = z + 1;

                // Y
                while (boundY < sizeY) {
                  final int tryIdx = boundY * sizeXZ + xSizeZ + z;
                  if (visited[tryIdx] || valueGrid[tryIdx] != value) break;
                  visited[tryIdx] = true;

                  groupIndices[groupIndicesSize++] = tryIdx;
                  boundY++;
                }

                // X
                extendX:
                while (boundX < sizeX) {
                  int i = 0;
                  for (int tryY = y; tryY < boundY; tryY++) {
                    final int tryIdx = tryY * sizeXZ + boundX * sizeZ + z;
                    if (visited[tryIdx] || valueGrid[tryIdx] != value) break extendX;
                    groupIndices[groupIndicesSize + i++] = tryIdx;
                  }
                  groupIndicesSize += i;
                  boundX++;
                }

                // Z
                extendZ:
                while (boundZ < sizeZ) {
                  int i = 0;
                  for (int tryY = y; tryY < boundY; tryY++) {
                    for (int tryX = x; tryX < boundX; tryX++) {
                      final int tryIdx = tryY * sizeXZ + tryX * sizeZ + boundZ;
                      if (visited[tryIdx] || valueGrid[tryIdx] != value) break extendZ;
                      groupIndices[groupIndicesSize + i++] = tryIdx;
                    }
                  }
                  groupIndicesSize += i;
                  boundZ++;
                }

                // Mark as visited
                for (int i = 0; i < groupIndicesSize; i++) {
                  visited[groupIndices[i]] = true;
                }
                result.add(new Run(value, x, y, z, boundX, boundY, boundZ));
              }
            }
          }

          return result;
        }
      };
}
