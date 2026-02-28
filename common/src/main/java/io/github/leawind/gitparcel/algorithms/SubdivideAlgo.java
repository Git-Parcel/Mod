package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;
import org.jetbrains.annotations.Range;

@FunctionalInterface
public interface SubdivideAlgo {
  /**
   * Subdivide the given parcel into smaller parcels.
   *
   * @param sizeX The size of the parcel in the X axis.
   * @param sizeY The size of the parcel in the Y axis.
   * @param sizeZ The size of the parcel in the Z axis.
   * @param values The values to use for the subdivided parcels.
   * @param factory The factory to use for creating the subdivided parcels.
   * @return The subdivided parcels.
   * @param <T> The type of the subdivided parcels.
   */
  <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
      @Range(from = 1, to = 16) int sizeX,
      @Range(from = 1, to = 16) int sizeY,
      @Range(from = 1, to = 16) int sizeZ,
      Values values,
      ResultFactory<T> factory);

  interface Values {
    /**
     * Get the value at the given position.
     *
     * <p>Note: The position is relative to the origin of the parcel.
     *
     * @return The value at the given position.
     */
    int get(
        @Range(from = 0, to = 15) int x,
        @Range(from = 0, to = 15) int y,
        @Range(from = 0, to = 15) int z);
  }

  interface ResultFactory<T> {
    T create(
        int value,
        @Range(from = 0, to = 15) int originX,
        @Range(from = 0, to = 15) int originY,
        @Range(from = 0, to = 15) int originZ,
        @Range(from = 1, to = 16) int sizeX,
        @Range(from = 1, to = 16) int sizeY,
        @Range(from = 1, to = 16) int sizeZ);
  }

  SubdivideAlgo V1 =
      new SubdivideAlgo() {

        @Override
        public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
            int sizeX, int sizeY, int sizeZ, Values values, ResultFactory<T> factory) {

          final int sizeXZ = sizeX * sizeZ;
          final int totalSize = sizeY * sizeXZ;

          ArrayList<T> result = new ArrayList<>(totalSize / 2 + 1);

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

          for (int y = 0; y < sizeY; y++) {
            final int yOffset = y * sizeXZ;

            for (int x = 0; x < sizeX; x++) {
              final int xSizeZ = x * sizeZ;
              final int xOffset = yOffset + xSizeZ;

              for (int z = 0; z < sizeZ; z++) {
                final int idx = xOffset + z;

                if (visited[idx]) continue;

                final int value = valueGrid[idx];

                int boundY = y + 1;
                int boundX = x + 1;
                int boundZ = z + 1;

                // Y
                while (boundY < sizeY) {
                  final int tryIdx = boundY * sizeXZ + xSizeZ + z;
                  if (visited[tryIdx] || valueGrid[tryIdx] != value) break;
                  boundY++;
                }

                // X
                extendX:
                while (boundX < sizeX) {
                  for (int tryY = y; tryY < boundY; tryY++) {
                    final int tryIdx = tryY * sizeXZ + boundX * sizeZ + z;
                    if (visited[tryIdx] || valueGrid[tryIdx] != value) break extendX;
                  }
                  boundX++;
                }

                // Z
                extendZ:
                while (boundZ < sizeZ) {
                  for (int tryY = y; tryY < boundY; tryY++) {
                    for (int tryX = x; tryX < boundX; tryX++) {
                      final int tryIdx = tryY * sizeXZ + tryX * sizeZ + boundZ;
                      if (visited[tryIdx] || valueGrid[tryIdx] != value) break extendZ;
                    }
                  }
                  boundZ++;
                }

                // Mark as visited
                for (int cy = y; cy < boundY; cy++) {
                  final int cyOffset = cy * sizeXZ;
                  for (int cx = x; cx < boundX; cx++) {
                    final int cxOffset = cyOffset + cx * sizeZ;
                    for (int cz = z; cz < boundZ; cz++) {
                      visited[cxOffset + cz] = true;
                    }
                  }
                }
                result.add(factory.create(value, x, y, z, boundX - x, boundY - y, boundZ - z));
              }
            }
          }

          return result;
        }
      };

  SubdivideAlgo V2 =
      new SubdivideAlgo() {

        @Override
        public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
            int sizeX, int sizeY, int sizeZ, Values values, ResultFactory<T> factory) {

          final int sizeXZ = sizeX * sizeZ;
          final int totalSize = sizeY * sizeXZ;

          ArrayList<T> result = new ArrayList<>(totalSize / 2 + 1);

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
                result.add(factory.create(value, x, y, z, boundX - x, boundY - y, boundZ - z));
              }
            }
          }

          return result;
        }
      };
}
