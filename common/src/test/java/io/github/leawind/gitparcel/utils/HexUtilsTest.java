package io.github.leawind.gitparcel.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.utils.hex.HexUtils;
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
}
