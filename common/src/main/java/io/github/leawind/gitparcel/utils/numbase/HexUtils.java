package io.github.leawind.gitparcel.utils.numbase;

import org.jetbrains.annotations.Range;

public final class HexUtils {
  /** Array of uppercase hexadecimal digits (0-9, A-F). */
  public static final char[] UPPERS = "0123456789ABCDEF".toCharArray();

  /** Cache of 256 precomputed uppercase hexadecimal strings for byte values (0x00 to 0xFF). */
  public static final String[] BYTE_TO_HEX_UPPER = new String[256];

  /** Array of lowercase hexadecimal digits (0-9, a-f). */
  public static final char[] LOWERS = "0123456789abcdef".toCharArray();

  /** Cache of 256 precomputed lowercase hexadecimal strings for byte values (0x00 to 0xFF). */
  public static final String[] BYTE_TO_HEX_LOWER = new String[256];

  static {
    for (int i = 0; i < 256; i++) {
      char[] chars = new char[2];

      chars[0] = UPPERS[(i >> 4) & 0xF];
      chars[1] = UPPERS[i & 0xF];
      BYTE_TO_HEX_UPPER[i] = new String(chars);

      chars[0] = LOWERS[(i >> 4) & 0xF];
      chars[1] = LOWERS[i & 0xF];
      BYTE_TO_HEX_LOWER[i] = new String(chars);
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
    return BYTE_TO_HEX_UPPER[b];
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
    return BYTE_TO_HEX_LOWER[b];
  }

  /**
   * Converts an integer to its uppercase hexadecimal string representation. The result does not
   * include leading zeros and uses uppercase letters (A-F).
   *
   * @param i Integer value to convert (must be non-negative)
   * @return Hexadecimal string representation in uppercase
   */
  public static String toHexUpperCase(@Range(from = 0x00, to = Integer.MAX_VALUE) int i) {
    if (i == 0) {
      return "0";
    }
    char[] buf = new char[8];
    int pos = 8;
    while (i != 0) {
      buf[--pos] = UPPERS[i & 0xF];
      i >>>= 4;
    }
    return new String(buf, pos, 8 - pos);
  }

  /**
   * Converts a long integer to its lowercase hexadecimal string representation. The result does not
   * include leading zeros and uses lowercase letters (a-f).
   *
   * @param i Long integer value to convert (must be non-negative)
   * @return Hexadecimal string representation in lowercase
   */
  public static String toHexLowerCase(@Range(from = 0x00, to = Long.MAX_VALUE) long i) {
    if (i == 0L) {
      return "0";
    }
    char[] buf = new char[16];
    int pos = 16;
    while (i != 0L) {
      buf[--pos] = LOWERS[(int) (i & 0xF)];
      i >>>= 4;
    }
    return new String(buf, pos, 16 - pos);
  }

  /**
   * Converts a long integer to its uppercase hexadecimal string representation. The result does not
   * include leading zeros and uses uppercase letters (A-F).
   *
   * @param i Long integer value to convert (must be non-negative)
   * @return Hexadecimal string representation in uppercase
   */
  public static String toHexUpperCase(@Range(from = 0x00, to = Long.MAX_VALUE) long i) {
    if (i == 0L) {
      return "0";
    }
    char[] buf = new char[16];
    int pos = 16;
    while (i != 0L) {
      buf[--pos] = UPPERS[(int) (i & 0xF)];
      i >>>= 4;
    }
    return new String(buf, pos, 16 - pos);
  }
}
