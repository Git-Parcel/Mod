package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3i;

public class ParcellaV0 implements ParcelFormat {

  @Override
  public String id() {
    return "parcella";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends ParcellaV0 implements ParcelFormat.Save {
    @Override
    public void save(
        ServerLevel level,
        BlockPos from,
        Vec3i size,
        Path dir,
        boolean includeBlock,
        boolean includeEntity)
        throws IOException {
      // NOW
      throw new RuntimeException("Not implemented");
    }
  }

  /**
   * Utility class for 3D Z-Order (Morton) curve operations. Provides methods for encoding 3D
   * coordinates to 1D indices and decoding them back. The Z-Order curve preserves spatial locality
   * by interleaving the bits of coordinate values.
   */
  public static class ZOrder3D {

    /**
     * Converts 3D coordinates to a 1D Z-Order index using Vector3i. This method only accepts
     * non-negative coordinates.
     *
     * @param coord the 3D coordinate to encode
     * @return the corresponding 1D Z-Order index
     * @throws IllegalArgumentException if any coordinate is negative
     */
    public static long coordToIndex(Vector3i coord) {
      return coordToIndex(coord.x, coord.y, coord.z);
    }

    /**
     * Converts 3D coordinates to a 1D Z-Order index using Vec3i from Minecraft. This method only
     * accepts non-negative coordinates.
     *
     * @param coord the 3D coordinate to encode
     * @return the corresponding 1D Z-Order index
     * @throws IllegalArgumentException if any coordinate is negative
     */
    public static long coordToIndex(Vec3i coord) {
      return coordToIndex(coord.getX(), coord.getY(), coord.getZ());
    }

    /**
     * Converts 3D coordinates to a 1D Z-Order index. This method only accepts non-negative
     * coordinates.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the corresponding 1D Z-Order index
     * @throws IllegalArgumentException if any coordinate is negative
     */
    public static long coordToIndex(long x, long y, long z) {
      if (x < 0 || y < 0 || z < 0) {
        throw new IllegalArgumentException(
            "Coordinates must be non-negative for standard Z-order encoding.");
      }

      x = part1By2(x);
      y = part1By2(y);
      z = part1By2(z);

      return x | (y << 1) | (z << 2);
    }

    /**
     * Converts a 1D Z-Order index back to 3D coordinates. This method decodes indices that were
     * created with non-negative coordinates.
     *
     * @param index the 1D Z-Order index to decode
     * @return the corresponding 3D coordinate as a Vector3i
     */
    public static Vector3i indexToCoord(long index) {
      int x = (int) compact1By2(index);
      int y = (int) compact1By2(index >> 1);
      int z = (int) compact1By2(index >> 2);
      return new Vector3i(x, y, z);
    }

    /**
     * Converts a signed 1D Z-Order index back to 3D coordinates that may include negative values.
     * This method handles coordinates in all 8 octants by using a sign/magnitude representation.
     *
     * @param index the 1D Z-Order index to decode (includes sign information)
     * @return the corresponding 3D coordinate as a Vector3i that may contain negative values
     */
    public static Vector3i indexToCoordSigned(long index) {
      long j = index / 8;
      Vector3i coord = indexToCoord(j);
      int k = (int) (index % 8);
      var x = SIGN_OFFSET[k];
      Vector3i sign = x[0];
      Vector3i offset = x[1];

      return coord.mul(sign).add(offset);
    }

    /**
     * Converts 3D coordinates to a signed 1D Z-Order index using Vector3i. This method accepts
     * coordinates that may be negative by using a sign/magnitude representation.
     *
     * @param coord the 3D coordinate to encode (may contain negative values)
     * @return the corresponding 1D Z-Order index with sign information encoded
     */
    public static long coordToIndexSigned(Vector3i coord) {
      return coordToIndexSigned(coord.x, coord.y, coord.z);
    }

    /**
     * Converts 3D coordinates to a signed 1D Z-Order index using Vec3i from Minecraft. This method
     * accepts coordinates that may be negative by using a sign/magnitude representation.
     *
     * @param coord the 3D coordinate to encode (may contain negative values)
     * @return the corresponding 1D Z-Order index with sign information encoded
     */
    public static long coordToIndexSigned(Vec3i coord) {
      return coordToIndexSigned(coord.getX(), coord.getY(), coord.getZ());
    }

    /**
     * Converts 3D coordinates to a signed 1D Z-Order index. This method accepts coordinates that
     * may be negative by using a sign/magnitude representation.
     *
     * @param x the X coordinate (may be negative)
     * @param y the Y coordinate (may be negative)
     * @param z the Z coordinate (may be negative)
     * @return the corresponding 1D Z-Order index with sign information encoded
     */
    public static long coordToIndexSigned(int x, int y, int z) {
      int cx = (x >= 0) ? x : (-x - 1);
      int cy = (y >= 0) ? y : (-y - 1);
      int cz = (z >= 0) ? z : (-z - 1);

      int k = 0;
      if (x < 0) k |= 0b100;
      if (y < 0) k |= 0b010;
      if (z < 0) k |= 0b001;

      return (coordToIndex(cx, cy, cz) << 3) | k;
    }

    /**
     * Alternative implementation for converting 3D coordinates to a signed 1D Z-Order index. This
     * method accepts coordinates that may be negative by using a sign/magnitude representation.
     * This is an alternative algorithm that searches through octants to find the correct
     * transformation.
     *
     * @param coord the 3D coordinate to encode (may contain negative values)
     * @return the corresponding 1D Z-Order index with sign information encoded
     * @throws IllegalArgumentException if the coordinate cannot be transformed to a valid positive
     *     coordinate
     */
    @SuppressWarnings("unused")
    public static long coordToIndexSigned2(Vector3i coord) {
      for (int k = 0; k < 8; k++) {
        Vector3i sign = SIGN_OFFSET[k][0];
        Vector3i offset = SIGN_OFFSET[k][1];

        Vector3i base = new Vector3i(coord).sub(offset).mul(sign);

        if (base.x >= 0 && base.y >= 0 && base.z >= 0) {
          long j = coordToIndex(base);
          return j * 8 + k;
        }
      }

      throw new IllegalArgumentException("Invalid coordinate for signed Z-order encoding.");
    }

    private static long part1By2(long i) {
      i &= 0x00000000001fffffL;
      i = (i ^ (i << 32)) & 0x1f00000000ffffL;
      i = (i ^ (i << 16)) & 0x1f0000ff0000ffL;
      i = (i ^ (i << 8)) & 0x100f00f00f00f00fL;
      i = (i ^ (i << 4)) & 0x10c30c30c30c30c3L;
      i = (i ^ (i << 2)) & 0x1249249249249249L;
      return i;
    }

    private static long compact1By2(long i) {
      i &= 0x1249249249249249L;
      i = (i ^ (i >> 2)) & 0x10c30c30c30c30c3L;
      i = (i ^ (i >> 4)) & 0x100f00f00f00f00fL;
      i = (i ^ (i >> 8)) & 0x1f0000ff0000ffL;
      i = (i ^ (i >> 16)) & 0x1f00000000ffffL;
      i = (i ^ (i >> 32)) & 0x00000000001fffffL;
      return i;
    }

    private static final Vector3i[][] SIGN_OFFSET =
        new Vector3i[][] {
          new Vector3i[] {new Vector3i(1, 1, 1), new Vector3i(0, 0, 0)},
          new Vector3i[] {new Vector3i(1, 1, -1), new Vector3i(0, 0, -1)},
          new Vector3i[] {new Vector3i(1, -1, 1), new Vector3i(0, -1, 0)},
          new Vector3i[] {new Vector3i(1, -1, -1), new Vector3i(0, -1, -1)},
          new Vector3i[] {new Vector3i(-1, 1, 1), new Vector3i(-1, 0, 0)},
          new Vector3i[] {new Vector3i(-1, 1, -1), new Vector3i(-1, 0, -1)},
          new Vector3i[] {new Vector3i(-1, -1, 1), new Vector3i(-1, -1, 0)},
          new Vector3i[] {new Vector3i(-1, -1, -1), new Vector3i(-1, -1, -1)},
        };
  }
}
