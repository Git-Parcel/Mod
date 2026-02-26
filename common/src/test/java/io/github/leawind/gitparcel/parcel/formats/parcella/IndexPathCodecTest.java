package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

public class IndexPathCodecTest {

  @Test
  void testIndexToPath() {
    var cwd = Path.of(".");
    BiConsumer<Long, String> test =
        (index, path) -> {
          assert IndexPathCodec.indexToPath(cwd, index, ".dat").equals(cwd.resolve(path));
        };
    test.accept(0x1234L, "34/12.dat");

    test.accept(0x00L, "00.dat");
    test.accept(0x01L, "01.dat");
    test.accept(0xFFL, "FF.dat");

    test.accept(0x0FL, "0F.dat");

    test.accept(0x240FL, "0F/24.dat");

    test.accept(0x01240FL, "0F/24/01.dat");
    test.accept(0x31240FL, "0F/24/31.dat");
  }
}
