package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.utils.hex.HexUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for encoding a numeric index into a hierarchical {@link Path}.
 *
 * <p>The index is decomposed into unsigned bytes (least significant byte first), each represented
 * as a two-character uppercase hexadecimal string. Each byte becomes one path segment. This
 * produces a directory structure that distributes entries across multiple levels to avoid large
 * flat directories.
 *
 * <p>For example, an index value is split into {@code index & 0xFF} chunks, and each chunk is
 * converted to a hexadecimal string such as {@code "0A"} or {@code "FF"}.
 *
 * <p>Small index values are served from an internal cache to reduce allocation overhead.
 *
 * <p>This class is not intended to be instantiated.
 */
public final class IndexPathCodec {

  /**
   * Encodes the given {@code index} into a hierarchical {@link Path}, appending the provided {@code
   * suffix} to the final segment.
   *
   * <p>The index is split into unsigned bytes (least significant first), each rendered as a
   * two-digit uppercase hexadecimal string and joined using the platform-specific file separator.
   *
   * @param index the numeric index to encode
   * @param suffix the suffix to append to the final path (e.g. a file extension)
   * @return a {@link Path} representing the encoded index
   * @throws IndexOutOfBoundsException if the index is negative
   */
  static Path indexToPath(long index, String suffix) throws IndexOutOfBoundsException {
    var parts = indexToPathParts(index);
    return Path.of(String.join(File.separator, parts) + suffix);
  }

  /**
   * Returns the hexadecimal path segments corresponding to the given {@code index}.
   *
   * <p>If the index is smaller than the internal cache size, a cached representation is returned.
   * Otherwise, the value is computed on demand.
   *
   * @param index the numeric index to encode
   * @return an ordered list of hexadecimal path segments (least significant byte first)
   * @throws IndexOutOfBoundsException if the index is negative
   */
  static List<String> indexToPathParts(long index) throws IndexOutOfBoundsException {
    if (index < CACHE_SIZE) {
      return Arrays.asList(CACHE[(int) index]);
    }
    return indexToPathPartsImpl(index);
  }

  /**
   * Computes the hexadecimal path segments for the given {@code index} without using the cache.
   *
   * <p>Behavior is undefined if the index is negative.
   *
   * <p>The index is decomposed into unsigned bytes (least significant first). Each byte is
   * converted to a two-digit uppercase hexadecimal string.
   *
   * <p>A zero index produces a single segment {@code "00"}.
   *
   * @param index the numeric index to encode. Must be non-negative.
   * @return a list of hexadecimal path segments (least significant byte first)
   */
  static List<String> indexToPathPartsImpl(long index) {
    if (index == 0) {
      return List.of("00");
    }

    var parts = new ArrayList<String>(32);
    do {
      parts.add(HexUtils.byteToHexUpperCase((int) (index & 0xFF)));
      index >>>= 8;
    } while (index != 0);
    return parts;
  }

  private static final int CACHE_SIZE = 512;

  /** Cache of precomputed hexadecimal path segments for indices. */
  private static final String[][] CACHE = new String[CACHE_SIZE][];

  static {
    for (int i = 0; i < CACHE_SIZE; i++) {
      List<String> parts = indexToPathPartsImpl(i);
      CACHE[i] = parts.toArray(new String[0]);
    }
  }
}
