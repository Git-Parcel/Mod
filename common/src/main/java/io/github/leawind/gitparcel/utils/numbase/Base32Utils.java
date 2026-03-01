package io.github.leawind.gitparcel.utils.numbase;

import org.jetbrains.annotations.Range;

/**
 * Utility class for base32 encoding and decoding operations. Uses standard base32 character set:
 * 0-9, A-V.
 */
public final class Base32Utils {
  /** Array of base32 digits (0-9, A-V). */
  public static final char[] BASE32_DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUV".toCharArray();

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
      buf[--pos] = BASE32_DIGITS[i & 0x1F];
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
      buf[--pos] = BASE32_DIGITS[(int) (i & 0x1F)];
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
}
