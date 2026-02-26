package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class IndexPathCodec {

  static Path indexToPath(Path root, long index, String suffix) {
    if (index == 0) {
      return root.resolve("00" + suffix);
    }

    Path result = root;
    long value = index;

    List<String> parts = new ArrayList<>();
    while (value != 0) {
      int b = (int) (value & 0xFF);
      parts.add(String.format("%02X", b));
      value >>>= 8;
    }

    int last = parts.size() - 1;
    for (int i = 0; i < last; i++) {
      result = result.resolve(parts.get(i));
    }

    return result.resolve(parts.get(last) + suffix);
  }
}
