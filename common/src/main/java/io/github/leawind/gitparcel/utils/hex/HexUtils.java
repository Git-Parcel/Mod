package io.github.leawind.gitparcel.utils.hex;

import org.jetbrains.annotations.Range;

public final class HexUtils {
  private static final char[] UPPER_HEX_DIGITS = "0123456789ABCDEF".toCharArray();
  private static final String[] BYTES_UPPER_CACHE = new String[256];
  private static final char[] LOWER_HEX_DIGITS = "0123456789abcdef".toCharArray();
  private static final String[] BYTES_LOWER_CACHE = new String[256];

  static {
    for (int i = 0; i < 256; i++) {
      char[] chars = new char[2];

      chars[0] = UPPER_HEX_DIGITS[(i >> 4) & 0xF];
      chars[1] = UPPER_HEX_DIGITS[i & 0xF];
      BYTES_UPPER_CACHE[i] = new String(chars);

      chars[0] = LOWER_HEX_DIGITS[(i >> 4) & 0xF];
      chars[1] = LOWER_HEX_DIGITS[i & 0xF];
      BYTES_LOWER_CACHE[i] = new String(chars);
    }
  }

  /**
   * Converts a byte to a 2-character hex string.
   *
   * @param b Byte to convert
   * @return 2-character hex string
   * @throws IndexOutOfBoundsException if b is greater than 255
   */
  public static String byteToHexUpperCase(@Range(from = 0x00, to = 0xFF) int b)
      throws IndexOutOfBoundsException {
    return BYTES_UPPER_CACHE[b];
  }

  /**
   * Converts a byte to a 2-character hex string (Lower case)
   *
   * @param b Byte to convert
   * @return 2-character hex string
   * @throws IndexOutOfBoundsException if b is greater than 255
   */
  public static String byteToHexLowerCase(@Range(from = 0x00, to = 0xFF) int b)
      throws IndexOutOfBoundsException {
    return BYTES_LOWER_CACHE[b];
  }

  public static String toHexUpperCase(@Range(from = 0x00, to = Integer.MAX_VALUE) int i) {
    if (i == 0) {
      return "0";
    }
    char[] buf = new char[8];
    int pos = 8;
    while (i != 0) {
      buf[--pos] = UPPER_HEX_DIGITS[i & 0xF];
      i >>>= 4;
    }
    return new String(buf, pos, 8 - pos);
  }

  public static String toHexLowerCase(@Range(from = 0x00, to = Long.MAX_VALUE) long i) {
    if (i == 0L) {
      return "0";
    }
    char[] buf = new char[16];
    int pos = 16;
    while (i != 0L) {
      buf[--pos] = LOWER_HEX_DIGITS[(int) (i & 0xF)];
      i >>>= 4;
    }
    return new String(buf, pos, 16 - pos);
  }

  public static String toHexUpperCase(@Range(from = 0x00, to = Long.MAX_VALUE) long i) {
    if (i == 0L) {
      return "0";
    }
    char[] buf = new char[16];
    int pos = 16;
    while (i != 0L) {
      buf[--pos] = UPPER_HEX_DIGITS[(int) (i & 0xF)];
      i >>>= 4;
    }
    return new String(buf, pos, 16 - pos);
  }
}
