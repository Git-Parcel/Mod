package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.nio.file.Path;

public final class RadixTreePathGenerator {
  private RadixTreePathGenerator() {}

  public static Path toPath(Path root, long index, String suffix) throws IndexOutOfBoundsException {
    if (index < 0) {
      throw new IndexOutOfBoundsException("index must be non-negative");
    }
    Path result = root;
    do {
      String part = HexUtils.byteToHexUpperCase((int) (index & 0xFF));
      index >>>= 8;
      result = result.resolve(index == 0 ? part + suffix : part);
    } while (index != 0);
    return result;
  }

}
