package io.github.leawind.gitparcel.builtin.parcella.utils;

import io.github.leawind.gitparcel.core.util.numbase.HexUtils;
import java.io.File;
import java.nio.file.Path;

public class RadixTreePathGenerator {

  public static Path toPath(Path root, long index, String suffix) throws IndexOutOfBoundsException {
    return root.resolve(toRelativePath(index) + suffix);
  }

  private static String toRelativePath(long index) {
    if (index < CACHE_SIZE) {
      return CACHE[(int) index];
    }
    return toRelativePathImpl(index);
  }

  private static String toRelativePathImpl(long index) {
    if (index == 0) {
      return "00";
    }

    StringBuilder builder = new StringBuilder(32);
    do {
      if (!builder.isEmpty()) {
        builder.append(File.separator);
      }

      builder.append(HexUtils.byteToHexUpperCase((int) (index & 0xFF)));
      index >>>= 8;
    } while (index != 0);

    return builder.toString();
  }

  private static final int CACHE_SIZE = 768;

  private static final String[] CACHE = new String[CACHE_SIZE];

  static {
    for (int i = 0; i < CACHE_SIZE; i++) {
      CACHE[i] = toRelativePathImpl(i);
    }
  }
}
