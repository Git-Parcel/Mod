package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class IndexPathCodec {
  static Path indexToPath(Path root, long index, String suffix) {
    return root.resolve(indexToPath(index, suffix));
  }

  static Path indexToPath(long index, String suffix) {
    var parts = indexToPathParts(index);
    return Path.of(String.join(File.separator, parts) + suffix);
  }

  static List<String> indexToPathParts(long index) {
    if (index < CACHE_SIZE) {
      return Arrays.asList(CACHE[(int) index]);
    }
    return indexToPathPartsImpl(index);
  }

  static List<String> indexToPathPartsImpl(long index) {
    if (index == 0) {
      return List.of("00");
    }

    var parts = new ArrayList<String>(32);
    do {
      parts.add(toHex((int) (index & 0xFF)));
      index >>>= 8;
    } while (index != 0);
    return parts;
  }

  static String toHex(int value) {
    return HEX_DIGITS[value];
  }

  static final String[] HEX_DIGITS = new String[0x100];

  static {
    for (int i = 0; i < 256; i++) {
      HEX_DIGITS[i] = String.format("%02X", i);
    }
  }

  private static final int CACHE_SIZE = 512;
  private static final String[][] CACHE = new String[CACHE_SIZE][];

  static {
    for (int i = 0; i < CACHE_SIZE; i++) {
      List<String> parts = indexToPathPartsImpl(i);
      CACHE[i] = parts.toArray(new String[0]);
    }
  }
}
