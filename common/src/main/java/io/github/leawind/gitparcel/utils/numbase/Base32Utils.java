package io.github.leawind.gitparcel.utils.numbase;

import org.jetbrains.annotations.Range;

/**
 * Utility class for base32 encoding and decoding operations. Uses standard base32 character set:
 * 0-9, A-V.
 */
public final class Base32Utils {
  /** Array of base32 digits (0-9, A-V). */
  public static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUV".toCharArray();

  /**
   * Maps base32 characters to their corresponding numeric values (0-31). Invalid characters map to
   * -1.
   */
  public static final byte[] BASE32_CHAR_TO_NUM = new byte[256];

  static {
    for (int i = 0; i < 256; i++) {
      BASE32_CHAR_TO_NUM[i] = -1;
    }
    byte i;
    byte b;
    for (i = 0, b = '0'; b <= '9'; i++, b++) {
      BASE32_CHAR_TO_NUM[b] = i;
    }
    for (i = 10, b = 'A'; b <= 'V'; i++, b++) {
      BASE32_CHAR_TO_NUM[b] = i;
    }
  }

  /**
   * Converts an integer to its base32 string representation. The result does not include leading
   * zeros.
   *
   * @param i Integer value to convert (must be non-negative)
   * @return Base32 string representation
   */
  public static String toBase32(@Range(from = 0x00, to = Integer.MAX_VALUE) int i) {
    if (i == 0) {
      return "0";
    }
    //  10: 2147483647
    //  16:   7FFFFFFF
    //  32:    1VVVVVV
    // 256:       ????
    char[] buf = new char[7];
    int pos = 7;

    while (i != 0) {
      buf[--pos] = CHARS[i & 0x1F];
      i >>>= 5;
    }

    return new String(buf, pos, 7 - pos);
  }

  /**
   * Converts a long integer to its base32 string representation. The result does not include
   * leading zeros.
   *
   * @param i Long integer value to convert (must be non-negative)
   * @return Base32 string representation
   */
  public static String toBase32(@Range(from = 0x00, to = Long.MAX_VALUE) long i) {
    if (i == 0L) {
      return "0";
    }

    //  10: 9223372036854775808 19
    //  16:    7FFFFFFFFFFFFFFF 16
    //  32:       7VVVVVVVVVVVV 13
    // 256:            ????????  8
    char[] buf = new char[13];
    int pos = 13;

    while (i != 0L) {
      buf[--pos] = CHARS[(int) (i & 0x1F)];
      i >>>= 5;
    }

    return new String(buf, pos, 13 - pos);
  }

  /**
   * Converts a base32 string back to an integer.
   *
   * @param base32Str Base32 string to convert
   * @return Integer value
   * @throws IllegalArgumentException if the string contains invalid base32 characters
   */
  public static int fromBase32(String base32Str) throws IllegalArgumentException {
    if (base32Str == null || base32Str.isEmpty()) {
      throw new IllegalArgumentException("Base32 string cannot be null or empty");
    }

    int result = 0;
    for (int i = 0; i < base32Str.length(); i++) {
      char c = base32Str.charAt(i);
      int digit = charToValue(c);

      // Check for overflow
      if (result > (Integer.MAX_VALUE - digit) / 32) {
        throw new IllegalArgumentException("Base32 string too large for integer");
      }

      result = result * 32 + digit;
    }

    return result;
  }

  /**
   * Converts a base32 string back to a long integer.
   *
   * @param base32Str Base32 string to convert
   * @return Long integer value
   * @throws IllegalArgumentException if the string contains invalid base32 characters
   */
  public static long fromBase32ToLong(String base32Str) throws IllegalArgumentException {
    if (base32Str == null || base32Str.isEmpty()) {
      throw new IllegalArgumentException("Base32 string cannot be null or empty");
    }

    long result = 0L;
    for (int i = 0; i < base32Str.length(); i++) {
      char c = base32Str.charAt(i);
      int digit = charToValue(c);

      // Check for overflow
      if (result > (Long.MAX_VALUE - digit) / 32) {
        throw new IllegalArgumentException("Base32 string too large for long integer");
      }

      result = result * 32 + digit;
    }

    return result;
  }

  /**
   * Converts a base32 character to its numeric value.
   *
   * @param c Character to convert
   * @return Numeric value (0-31)
   * @throws IllegalArgumentException if the character is not a valid base32 digit
   */
  private static int charToValue(char c) throws IllegalArgumentException {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'A' && c <= 'V') {
      return c - 'A' + 10;
    } else {
      throw new IllegalArgumentException("Invalid base32 character: " + c);
    }
  }

  public static byte parseChar(byte ch) {
    return BASE32_CHAR_TO_NUM[ch];
  }

  /**
   * Parses a positive base32 number from a byte array directly, without creating intermediate
   * Strings.
   *
   * <p>Returns -1 if an invalid character is encountered or if integer overflow occurs.
   *
   * <p><strong>Note:</strong> No bounds checking is performed on {@code offset} or {@code length}.
   *
   * @param bytes the byte array containing base32 digits
   * @param offset the starting index
   * @param length the number of bytes to parse
   * @return the parsed integer, or -1 on failure
   * @throws IndexOutOfBoundsException if indices are out of range
   */
  public static int parsePositive(byte[] bytes, int offset, int length) {
    int result = 0;
    final int limit = offset + length;

    for (int i = offset; i < limit; i++) {
      int digit = BASE32_CHAR_TO_NUM[bytes[i] & 0xFF];

      if (digit == -1) {
        return -1;
      }

      // Overflow check: ensure result * 32 + digit <= Integer.MAX_VALUE
      if (result > (Integer.MAX_VALUE - digit) / 32) {
        return -1;
      }

      result = (result * 32) + digit;
    }

    return result;
  }
}
