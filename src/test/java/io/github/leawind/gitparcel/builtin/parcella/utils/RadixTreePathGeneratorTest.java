package io.github.leawind.gitparcel.builtin.parcella.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

public class RadixTreePathGeneratorTest {

  @Test
  void testToPath() {
    Path workDir = Path.of("");
    BiConsumer<Long, String> test =
        (index, path) ->
            assertEquals(
              RadixTreePathGenerator.toPath(workDir, index, ".dat"), workDir.resolve(path));

    test.accept(0x1234L, "34/12.dat");

    test.accept(0x00L, "00.dat");
    test.accept(0x01L, "01.dat");
    test.accept(0x06L, "06.dat");
    test.accept(0x0FL, "0F.dat");
    test.accept(0xFFL, "FF.dat");

    test.accept(0x0100L, "00/01.dat");
    test.accept(0x0703L, "03/07.dat");
    test.accept(0x240FL, "0F/24.dat");
    test.accept(0xFF00L, "00/FF.dat");

    test.accept(0x01240FL, "0F/24/01.dat");
    test.accept(0x31240FL, "0F/24/31.dat");

    test.accept(0x010203040506L, "06/05/04/03/02/01.dat");
  }
}
