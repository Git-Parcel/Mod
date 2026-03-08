package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.api.parcel.Parcel;
import java.util.ArrayList;

@FunctionalInterface
public interface RunLengthEncoding3DAlgo {
  /**
   * Subdivide the given parcel into smaller parcels.
   *
   * @param sizeX The size of the X axis. Must be positive.
   * @param sizeY The size of the Y axis. Must be positive.
   * @param sizeZ The size of the Z axis. Must be positive.
   * @param values The values to use for the subdivided parcels.
   * @param factory The factory to use for creating the subdivided parcels.
   * @return The subdivided parcels.
   * @param <T> The type of the subdivided parcels.
   */
  <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
      int sizeX, int sizeY, int sizeZ, ValueGetter values, ResultFactory<T> factory);

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

  RunLengthEncoding3DAlgo V2 =
      new RunLengthEncoding3DAlgo() {

        @Override
        public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
            int sizeX, int sizeY, int sizeZ, ValueGetter values, ResultFactory<T> factory) {

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

  RunLengthEncoding3DAlgo INSTANCE = V2;
}
