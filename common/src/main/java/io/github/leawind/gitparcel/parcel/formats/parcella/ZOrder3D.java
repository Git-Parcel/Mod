package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.joml.Vector3i;

/**
 * Utility class for 3D Z-Order (Morton) curve operations. Provides methods for encoding 3D
 * coordinates to 1D indices and decoding them back. The Z-Order curve preserves spatial locality by
 * interleaving the bits of coordinate values.
 */
@SuppressWarnings("unused")
public class ZOrder3D {

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

    return part1By2(x) | (part1By2(y) << 1) | (part1By2(z) << 2);
  }

  /**
   * Converts a 1D Z-Order index back to 3D coordinates. This method decodes indices that were
   * created with non-negative coordinates.
   *
   * @param index the 1D Z-Order index to decode
   * @return the corresponding 3D coordinate as a Vector3i
   */
  public static Vector3i indexToCoord(long index) {
    return new Vector3i(
        (int) compact1By2(index), // >> 0
        (int) compact1By2(index >> 1),
        (int) compact1By2(index >> 2));
  }

  /**
   * Converts a signed 1D Z-Order index back to 3D coordinates that may include negative values.
   * This method handles coordinates in all 8 octants by using a sign/magnitude representation.
   *
   * @param index the 1D Z-Order index to decode (includes sign information)
   * @return the corresponding 3D coordinate as a Vector3i that may contain negative values
   */
  public static Vector3i indexToCoordSigned(long index) {
    if (index < CACHE_SIZE) {
      return INDEX_TO_COORD_CACHE[(int) index];
    }
    return indexToCoordSignedImpl(index);
  }

  /** Internal implementation of indexToCoordSigned without caching. */
  private static Vector3i indexToCoordSignedImpl(long index) {
    var x = SIGN_OFFSET[(int) (index % 8)];
    return indexToCoord(index / 8).mul(x[0]).add(x[1]);
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
   * Converts 3D coordinates to a signed 1D Z-Order index. This method accepts coordinates that may
   * be negative by using a sign/magnitude representation.
   *
   * @param x the X coordinate (may be negative)
   * @param y the Y coordinate (may be negative)
   * @param z the Z coordinate (may be negative)
   * @return the corresponding 1D Z-Order index with sign information encoded
   */
  public static long coordToIndexSigned(int x, int y, int z) {
    var cached = COORD_TO_INDEX_CACHE.get(hash(x, y, z));
    if (cached != null) {
      return cached;
    }
    return coordToIndexSignedImpl(x, y, z);
  }

  /** Internal implementation of coordToIndexSigned without caching. */
  private static long coordToIndexSignedImpl(int x, int y, int z) {
    int cx = (x >= 0) ? x : (-x - 1);
    int cy = (y >= 0) ? y : (-y - 1);
    int cz = (z >= 0) ? z : (-z - 1);

    int k = 0;
    if (x < 0) k |= 0b100;
    if (y < 0) k |= 0b010;
    if (z < 0) k |= 0b001;

    return (coordToIndex(cx, cy, cz) << 3) | k;
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

  private static long hash(int x, int y, int z) {
    final long PACKED_X_MASK = (1L << BlockPos.PACKED_HORIZONTAL_LENGTH) - 1L;
    final long PACKED_Y_MASK = (1L << BlockPos.PACKED_Y_LENGTH) - 1L;
    final long PACKED_Z_MASK = (1L << BlockPos.PACKED_HORIZONTAL_LENGTH) - 1L;
    final int Z_OFFSET = BlockPos.PACKED_Y_LENGTH;
    final int X_OFFSET = BlockPos.PACKED_Y_LENGTH + BlockPos.PACKED_HORIZONTAL_LENGTH;

    return ((long) x & PACKED_X_MASK) << X_OFFSET
        | ((long) y & PACKED_Y_MASK)
        | ((long) z & PACKED_Z_MASK) << Z_OFFSET;
  }

  private static final int CACHE_SIZE = 512;
  private static final Map<Long, Long> COORD_TO_INDEX_CACHE;
  private static final Vector3i[] INDEX_TO_COORD_CACHE;

  static {
    COORD_TO_INDEX_CACHE = new HashMap<>(CACHE_SIZE);
    INDEX_TO_COORD_CACHE = new Vector3i[CACHE_SIZE];
    for (int i = 0; i < CACHE_SIZE; i++) {
      var coord = indexToCoordSignedImpl(i);
      INDEX_TO_COORD_CACHE[i] = coord;
      BlockPos.ZERO.asLong();
      COORD_TO_INDEX_CACHE.put(hash(coord.x, coord.y, coord.z), (long) i);
    }
  }
}
