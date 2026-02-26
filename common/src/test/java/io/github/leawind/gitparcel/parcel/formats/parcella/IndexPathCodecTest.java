package io.github.leawind.gitparcel.parcel.formats.parcella;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

public class IndexPathCodecTest {

  @Test
  void testIndexToPathParts() {
    assertEquals(List.of("00"), IndexPathCodec.indexToPathParts(0x00L));
    assertEquals(List.of("01"), IndexPathCodec.indexToPathParts(0x01L));
    assertEquals(List.of("FF"), IndexPathCodec.indexToPathParts(0xFFL));
    assertEquals(List.of("34", "12"), IndexPathCodec.indexToPathParts(0x1234L));
    assertEquals(List.of("12", "34", "56"), IndexPathCodec.indexToPathParts(0x563412L));
    assertEquals(List.of("12", "34", "06"), IndexPathCodec.indexToPathParts(0x63412L));
    assertEquals(List.of("12", "34", "06", "FF"), IndexPathCodec.indexToPathParts(0xFF063412L));
    assertEquals(List.of("FF", "FF", "FF", "FF"), IndexPathCodec.indexToPathParts(0xFFFFFFFFL));
  }

  @Test
  void testIndexToPath2() {
    var cwd = Path.of(".");
    BiConsumer<Long, String> test =
        (index, path) ->
            assertEquals(IndexPathCodec.indexToPath(cwd, index, ".dat"), cwd.resolve(path));

    test.accept(0x1234L, "34/12.dat");

    test.accept(0x00L, "00.dat");
    test.accept(0x01L, "01.dat");
    test.accept(0xFFL, "FF.dat");

    test.accept(0x0FL, "0F.dat");

    test.accept(0x240FL, "0F/24.dat");

    test.accept(0x01240FL, "0F/24/01.dat");
    test.accept(0x31240FL, "0F/24/31.dat");
    test.accept(0x010203040506L, "06/05/04/03/02/01.dat");
  }
}
