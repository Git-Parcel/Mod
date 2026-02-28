package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;

@FunctionalInterface
public interface SubdivideAlgo {
  <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
      Parcel parcel, Values values, ResultFactory<T> factory);

  interface Values {
    int get(int x, int y, int z);
  }

  interface ResultFactory<T> {
    T create(int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ);
  }

  SubdivideAlgo V1 =
      new SubdivideAlgo() {

        @Override
        public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
            Parcel parcel, Values values, ResultFactory<T> factory) {

          final int sizeX = parcel.sizeX;
          final int sizeY = parcel.sizeY;
          final int sizeZ = parcel.sizeZ;
          final int originX = parcel.originX;
          final int originY = parcel.originY;
          final int originZ = parcel.originZ;

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
                valueGrid[xOffset + z] = values.get(originX + x, originY + y, originZ + z);
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
                result.add(
                    factory.create(
                        value,
                        originX + x,
                        originY + y,
                        originZ + z,
                        boundX - x,
                        boundY - y,
                        boundZ - z));
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
            Parcel parcel, Values values, ResultFactory<T> factory) {

          final int sizeX = parcel.sizeX;
          final int sizeY = parcel.sizeY;
          final int sizeZ = parcel.sizeZ;
          final int originX = parcel.originX;
          final int originY = parcel.originY;
          final int originZ = parcel.originZ;

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
                valueGrid[xOffset + z] = values.get(originX + x, originY + y, originZ + z);
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
                result.add(
                    factory.create(
                        value,
                        originX + x,
                        originY + y,
                        originZ + z,
                        boundX - x,
                        boundY - y,
                        boundZ - z));
              }
            }
          }

          return result;
        }
      };
}
