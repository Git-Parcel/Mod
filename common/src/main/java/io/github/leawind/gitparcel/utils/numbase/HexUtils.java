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

  /** Maps hexadecimal characters (0-9, A-F, a-f) to their corresponding numeric values (0-15). */
  public static final byte[] HEX_CHAR_TO_NUM = new byte[256];

  static {
    for (int i = 0; i < 256; i++) {
      char[] chars = new char[2];

      chars[0] = UPPERS[(i >> 4) & 0xF];
      chars[1] = UPPERS[i & 0xF];
      BYTE_TO_HEX_UPPER[i] = new String(chars);

      chars[0] = LOWERS[(i >> 4) & 0xF];
      chars[1] = LOWERS[i & 0xF];
      BYTE_TO_HEX_LOWER[i] = new String(chars);

      HEX_CHAR_TO_NUM[i] = -1;
    }
    HEX_CHAR_TO_NUM['0'] = 0;
    HEX_CHAR_TO_NUM['1'] = 1;
    HEX_CHAR_TO_NUM['2'] = 2;
    HEX_CHAR_TO_NUM['3'] = 3;
    HEX_CHAR_TO_NUM['4'] = 4;
    HEX_CHAR_TO_NUM['5'] = 5;
    HEX_CHAR_TO_NUM['6'] = 6;
    HEX_CHAR_TO_NUM['7'] = 7;
    HEX_CHAR_TO_NUM['8'] = 8;
    HEX_CHAR_TO_NUM['9'] = 9;
    HEX_CHAR_TO_NUM['A'] = 10;
    HEX_CHAR_TO_NUM['B'] = 11;
    HEX_CHAR_TO_NUM['C'] = 12;
    HEX_CHAR_TO_NUM['D'] = 13;
    HEX_CHAR_TO_NUM['E'] = 14;
    HEX_CHAR_TO_NUM['F'] = 15;
    HEX_CHAR_TO_NUM['a'] = 10;
    HEX_CHAR_TO_NUM['b'] = 11;
    HEX_CHAR_TO_NUM['c'] = 12;
    HEX_CHAR_TO_NUM['d'] = 13;
    HEX_CHAR_TO_NUM['e'] = 14;
    HEX_CHAR_TO_NUM['f'] = 15;
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

  /**
   * Converts a hexadecimal character (0-9, A-F, a-f) to its corresponding numeric value (0-15).
   *
   * @param ch Hexadecimal character to convert
   * @return Numeric value of the hexadecimal character, or -1 if invalid
   */
  public static byte parseChar(char ch) {
    return HEX_CHAR_TO_NUM[ch];
  }

  /**
   * @see #parseChar(char)
   */
  public static byte parseChar(byte ch) {
    return HEX_CHAR_TO_NUM[ch];
  }

  /**
   * Parses a positive hexadecimal number from a byte array without creating intermediate Strings.
   *
   * <p>Returns -1 if an invalid character is encountered or if integer overflow occurs.
   *
   * <p><strong>Note:</strong> No bounds checking is performed on {@code offset} or {@code length}.
   *
   * @param array the byte array containing hex digits
   * @param offset the starting index
   * @param length the number of bytes to parse
   * @return the parsed integer, or -1 on failure
   * @throws IndexOutOfBoundsException if indices are out of range
   */
  public static int parsePositive(byte[] array, int offset, int length)
      throws IndexOutOfBoundsException {
    int result = 0;
    final int limit = offset + length;

    for (int i = offset; i < limit; i++) {
      byte b = array[i];

      int digit = HEX_CHAR_TO_NUM[b & 0xFF];

      if (digit == -1) {
        return -1;
      }

      if (result > 0x0FFFFFFF || (result == 0x0FFFFFFF && digit > 7)) {
        return -1;
      }

      result = (result << 4) | digit;
    }

    return result;
  }
}
