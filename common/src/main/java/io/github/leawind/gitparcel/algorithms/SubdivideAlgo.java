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

          ArrayList<T> result = new ArrayList<>();
          int sizeX = parcel.sizeX;
          int sizeY = parcel.sizeY;
          int sizeZ = parcel.sizeZ;
          int originX = parcel.originX;
          int originY = parcel.originY;
          int originZ = parcel.originZ;

          int[] valueGridFlat = new int[sizeX * sizeY * sizeZ];
          for (int z = 0; z < sizeZ; z++) {
            int baseZY = z * sizeY * sizeX;
            for (int y = 0; y < sizeY; y++) {
              int baseYX = baseZY + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                valueGridFlat[baseYX + x] = values.get(originX + x, originY + y, originZ + z);
              }
            }
          }

          boolean[] visited = new boolean[sizeX * sizeY * sizeZ];

          for (int z = 0; z < sizeZ; z++) {
            int offsetZ = z * sizeX * sizeY;
            for (int y = 0; y < sizeY; y++) {
              int offsetY = offsetZ + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                int idx = offsetY + x;
                if (!visited[idx]) {
                  int value = valueGridFlat[idx];

                  int maxX = x;
                  while (maxX + 1 < sizeX) {
                    int nextIdx = offsetY + (maxX + 1);
                    if (visited[nextIdx] || valueGridFlat[nextIdx] != value) break;
                    maxX++;
                  }
                  int lenX = maxX - x + 1;

                  int maxY = y;
                  boolean canExtendY = true;
                  while (canExtendY && maxY + 1 < sizeY) {
                    int nextRowOffset = offsetZ + (maxY + 1) * sizeX;
                    for (int curX = x; curX <= maxX; curX++) {
                      int checkIdx = nextRowOffset + curX;
                      if (visited[checkIdx] || valueGridFlat[checkIdx] != value) {
                        canExtendY = false;
                        break;
                      }
                    }
                    if (canExtendY) maxY++;
                  }
                  int lenY = maxY - y + 1;

                  int maxZ = z;
                  boolean canExtendZ = true;
                  while (canExtendZ && maxZ + 1 < sizeZ) {
                    int nextSliceOffset = (maxZ + 1) * sizeX * sizeY;
                    outerZ:
                    for (int curY = y; curY <= maxY; curY++) {
                      int rowOffset = nextSliceOffset + curY * sizeX;
                      for (int curX = x; curX <= maxX; curX++) {
                        int checkIdx = rowOffset + curX;
                        if (visited[checkIdx] || valueGridFlat[checkIdx] != value) {
                          canExtendZ = false;
                          break outerZ;
                        }
                      }
                    }
                    if (canExtendZ) maxZ++;
                  }
                  int lenZ = maxZ - z + 1;

                  for (int cz = z; cz <= maxZ; cz++) {
                    int sliceOffset = cz * sizeX * sizeY;
                    for (int cy = y; cy <= maxY; cy++) {
                      int rowOffset = sliceOffset + cy * sizeX;
                      for (int cx = x; cx <= maxX; cx++) {
                        visited[rowOffset + cx] = true;
                      }
                    }
                  }

                  result.add(
                      factory.create(
                          value, originX + x, originY + y, originZ + z, lenX, lenY, lenZ));
                }
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

          ArrayList<T> result = new ArrayList<>();
          int sizeX = parcel.sizeX;
          int sizeY = parcel.sizeY;
          int sizeZ = parcel.sizeZ;
          int originX = parcel.originX;
          int originY = parcel.originY;
          int originZ = parcel.originZ;

          int totalSize = sizeX * sizeY * sizeZ;
          int[] valueGrid = new int[totalSize];
          boolean[] visited = new boolean[totalSize];

          for (int z = 0; z < sizeZ; z++) {
            int zOffset = z * sizeX * sizeY;
            for (int y = 0; y < sizeY; y++) {
              int yOffset = zOffset + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                valueGrid[yOffset + x] = values.get(originX + x, originY + y, originZ + z);
              }
            }
          }

          for (int z = 0; z < sizeZ; z++) {
            int zOffset = z * sizeX * sizeY;
            for (int y = 0; y < sizeY; y++) {
              int yOffset = zOffset + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                int idx = yOffset + x;
                if (visited[idx]) continue;

                int value = valueGrid[idx];

                int maxX = x;
                while (maxX + 1 < sizeX) {
                  int nextIdx = yOffset + (maxX + 1);
                  if (visited[nextIdx] || valueGrid[nextIdx] != value) break;
                  maxX++;
                }

                int maxY = y;
                boolean canExtendY = true;
                while (canExtendY && maxY + 1 < sizeY) {
                  int nextYOffset = zOffset + (maxY + 1) * sizeX;
                  for (int curX = x; curX <= maxX; curX++) {
                    int checkIdx = nextYOffset + curX;
                    if (visited[checkIdx] || valueGrid[checkIdx] != value) {
                      canExtendY = false;
                      break;
                    }
                  }
                  if (canExtendY) maxY++;
                }

                int maxZ = z;
                outerZ:
                while (maxZ + 1 < sizeZ) {
                  int nextZOffset = (maxZ + 1) * sizeX * sizeY;
                  for (int curY = y; curY <= maxY; curY++) {
                    int rowOffset = nextZOffset + curY * sizeX;
                    for (int curX = x; curX <= maxX; curX++) {
                      int checkIdx = rowOffset + curX;
                      if (visited[checkIdx] || valueGrid[checkIdx] != value) {
                        break outerZ;
                      }
                    }
                  }
                  maxZ++;
                }

                int lenX = maxX - x + 1;
                int lenY = maxY - y + 1;
                int lenZ = maxZ - z + 1;

                for (int cz = z; cz <= maxZ; cz++) {
                  int sliceOffset = cz * sizeX * sizeY;
                  for (int cy = y; cy <= maxY; cy++) {
                    int rowOffset = sliceOffset + cy * sizeX;
                    for (int cx = x; cx <= maxX; cx++) {
                      visited[rowOffset + cx] = true;
                    }
                  }
                }

                result.add(
                    factory.create(value, originX + x, originY + y, originZ + z, lenX, lenY, lenZ));
              }
            }
          }

          return result;
        }
      };

  SubdivideAlgo V3 =
      new SubdivideAlgo() {
        @Override
        public <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
            Parcel parcel, Values values, ResultFactory<T> factory) {

          int sizeX = parcel.sizeX;
          int sizeY = parcel.sizeY;
          int originX = parcel.originX;
          int sizeZ = parcel.sizeZ;
          int originY = parcel.originY;
          int originZ = parcel.originZ;

          int sizeXY = sizeX * sizeY;
          int totalSize = sizeXY * sizeZ;
          ArrayList<T> result = new ArrayList<>(totalSize);

          int[] valueGrid = new int[totalSize];

          for (int z = 0; z < sizeZ; z++) {
            int zOffset = z * sizeXY;
            for (int y = 0; y < sizeY; y++) {
              int yOffset = zOffset + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                valueGrid[yOffset + x] = values.get(originX + x, originY + y, originZ + z);
              }
            }
          }

          boolean[] visited = new boolean[totalSize];

          for (int z = 0; z < sizeZ; z++) {
            int zOffset = z * sizeXY;
            for (int y = 0; y < sizeY; y++) {
              int yOffset = zOffset + y * sizeX;
              for (int x = 0; x < sizeX; x++) {
                int idx = yOffset + x;
                if (visited[idx]) continue;

                int value = valueGrid[idx];

                int maxX = x;
                while (maxX + 1 < sizeX) {
                  int nextIdx = yOffset + (maxX + 1);
                  if (visited[nextIdx] || valueGrid[nextIdx] != value) break;
                  maxX++;
                }

                int maxY = y;
                boolean canExtendY = true;
                while (canExtendY && maxY + 1 < sizeY) {
                  int nextYOffset = zOffset + (maxY + 1) * sizeX;

                  for (int curX = x; curX <= maxX; curX++) {
                    int checkIdx = nextYOffset + curX;
                    if (visited[checkIdx] || valueGrid[checkIdx] != value) {
                      canExtendY = false;
                      break;
                    }
                  }
                  if (canExtendY) maxY++;
                }

                int maxZ = z;
                outerZ:
                while (maxZ + 1 < sizeZ) {
                  int nextZOffset = (maxZ + 1) * sizeXY;

                  for (int curY = y; curY <= maxY; curY++) {
                    int rowOffset = nextZOffset + curY * sizeX;

                    for (int curX = x; curX <= maxX; curX++) {
                      int checkIdx = rowOffset + curX;
                      if (visited[checkIdx] || valueGrid[checkIdx] != value) {
                        break outerZ;
                      }
                    }
                  }
                  maxZ++;
                }

                int lenX = maxX - x + 1;
                int lenY = maxY - y + 1;
                int lenZ = maxZ - z + 1;

                for (int cz = z; cz <= maxZ; cz++) {
                  int sliceOffset = cz * sizeXY;
                  for (int cy = y; cy <= maxY; cy++) {
                    int rowOffset = sliceOffset + cy * sizeX;

                    for (int cx = x; cx <= maxX; cx++) {
                      visited[rowOffset + cx] = true;
                    }
                  }
                }

                result.add(
                    factory.create(value, originX + x, originY + y, originZ + z, lenX, lenY, lenZ));
              }
            }
          }

          return result;
        }
      };
}
