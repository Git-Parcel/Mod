package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

public class ParcellaV0Test {

  static class SaveTest {
    @Test
    void testIndexToPath() {
      var cwd = Path.of(".");
      BiConsumer<Long, String> test =
          (index, path) -> {
            assert ParcellaV0.Save.indexToPath(cwd, index).equals(cwd.resolve(path));
          };
      test.accept(0x1234L, "34/12.txt");

      test.accept(0x00L, "00.txt");
      test.accept(0x01L, "01.txt");
      test.accept(0xFFL, "FF.txt");

      test.accept(0x0FL, "0F.txt");

      test.accept(0x240FL, "0F/24.txt");

      test.accept(0x01240FL, "0F/24/01.txt");
      test.accept(0x31240FL, "0F/24/31.txt");
    }
  }
}
