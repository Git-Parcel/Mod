package io.github.leawind.gitparcel.utils.numbase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class HexUtilsTest {
  @Test
  void testByteToHex() {

    for (int i = -500; i < 0; i++) {
      int finalI = i;
      assertThrows(IndexOutOfBoundsException.class, () -> HexUtils.byteToHexLowerCase(finalI));
      assertThrows(IndexOutOfBoundsException.class, () -> HexUtils.byteToHexUpperCase(finalI));
    }
    for (int i = 0; i < 256; i++) {
      assertEquals(String.format("%02x", i), HexUtils.byteToHexLowerCase(i));
      assertEquals(String.format("%02X", i), HexUtils.byteToHexUpperCase(i));
    }
    for (int i = 256; i < 500; i++) {
      int finalI = i;
      assertThrows(IndexOutOfBoundsException.class, () -> HexUtils.byteToHexLowerCase(finalI));
      assertThrows(IndexOutOfBoundsException.class, () -> HexUtils.byteToHexUpperCase(finalI));
    }
  }

  @Test
  void testToHex() {
    for (long i = 0; i < 131072; i += 11) {
      assertEquals(String.format("%x", i), HexUtils.toHexLowerCase((int) i));
      assertEquals(String.format("%x", i), HexUtils.toHexLowerCase(i));

      assertEquals(String.format("%X", i), HexUtils.toHexUpperCase((int) i));
      assertEquals(String.format("%X", i), HexUtils.toHexUpperCase(i));
    }
  }

  @Test
  void testParsePositive() {
    for (int i = 0; i < 256; i++) {
      String hex = String.format("%02X", i);
      byte[] bytes = hex.getBytes();
      assertEquals(i, HexUtils.parsePositive(bytes, 0, bytes.length));
    }

    assertEquals(-1, HexUtils.parsePositive(new byte[] {'G', 1, 2, 4, 'Z', 'z'}, 0, 6));
  }
}
