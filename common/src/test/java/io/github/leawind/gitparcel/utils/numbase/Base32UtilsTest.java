package io.github.leawind.gitparcel.utils.numbase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class Base32UtilsTest {

  @Test
  void testToBase32Int() {
    // Test basic conversions
    assertEquals("0", Base32Utils.toBase32(0));
    assertEquals("1", Base32Utils.toBase32(1));
    assertEquals("9", Base32Utils.toBase32(9));
    assertEquals("A", Base32Utils.toBase32(10));
    assertEquals("F", Base32Utils.toBase32(15));
    assertEquals("G", Base32Utils.toBase32(16));
    assertEquals("V", Base32Utils.toBase32(31));

    // Test multi-digit conversions
    assertEquals("10", Base32Utils.toBase32(32));
    assertEquals("1V", Base32Utils.toBase32(63));
    assertEquals("20", Base32Utils.toBase32(64));
    assertEquals("100", Base32Utils.toBase32(1024));

    // Test edge cases
    assertEquals("1VVVVVV", Base32Utils.toBase32(Integer.MAX_VALUE));
  }

  @Test
  void testToBase32Long() {
    // Test basic conversions
    assertEquals("0", Base32Utils.toBase32(0L));
    assertEquals("1", Base32Utils.toBase32(1L));
    assertEquals("V", Base32Utils.toBase32(31L));

    // Test multi-digit conversions
    assertEquals("10", Base32Utils.toBase32(32L));
    assertEquals("100", Base32Utils.toBase32(1024L));

    // Test large values
    assertEquals("10000000000", Base32Utils.toBase32(1125899906842624L));
    assertEquals("7VVVVVVVVVVVV", Base32Utils.toBase32(Long.MAX_VALUE));
  }

  @Test
  void testFromBase32() {
    // Test basic conversions
    assertEquals(0, Base32Utils.fromBase32("0"));
    assertEquals(1, Base32Utils.fromBase32("1"));
    assertEquals(9, Base32Utils.fromBase32("9"));
    assertEquals(10, Base32Utils.fromBase32("A"));
    assertEquals(15, Base32Utils.fromBase32("F"));
    assertEquals(16, Base32Utils.fromBase32("G"));
    assertEquals(31, Base32Utils.fromBase32("V"));

    // Test multi-digit conversions
    assertEquals(32, Base32Utils.fromBase32("10"));
    assertEquals(63, Base32Utils.fromBase32("1V"));
    assertEquals(64, Base32Utils.fromBase32("20"));
    assertEquals(1024, Base32Utils.fromBase32("100"));

    // Test edge case
    assertEquals(Integer.MAX_VALUE, Base32Utils.fromBase32("1VVVVVV"));
  }

  @Test
  void testFromBase32ToLong() {
    // Test basic conversions
    assertEquals(0L, Base32Utils.fromBase32ToLong("0"));
    assertEquals(1L, Base32Utils.fromBase32ToLong("1"));
    assertEquals(31L, Base32Utils.fromBase32ToLong("V"));

    // Test multi-digit conversions
    assertEquals(32L, Base32Utils.fromBase32ToLong("10"));
    assertEquals(1024L, Base32Utils.fromBase32ToLong("100"));

    // Test large values
    assertEquals(1125899906842624L, Base32Utils.fromBase32ToLong("10000000000"));
    assertEquals(Long.MAX_VALUE, Base32Utils.fromBase32ToLong("7VVVVVVVVVVVV"));
  }

  @Test
  void testInvalidBase32Characters() {
    // Test invalid characters
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("W"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("X"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("Y"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("Z"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("a"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("z"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("@"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("-"));

    // Test mixed valid and invalid characters
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("12W3"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("ABCZ"));
  }

  @Test
  void testNullAndEmptyInput() {
    // Test null input
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32(null));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32ToLong(null));

    // Test empty input
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32(""));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32ToLong(""));
  }

  @Test
  void testOverflow() {
    // Test integer overflow
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("8000000"));
    assertThrows(IllegalArgumentException.class, () -> Base32Utils.fromBase32("VVVVVVV"));

    // Test long overflow
    assertThrows(
        IllegalArgumentException.class, () -> Base32Utils.fromBase32ToLong("8000000000000"));
    assertThrows(
        IllegalArgumentException.class, () -> Base32Utils.fromBase32ToLong("VVVVVVVVVVVVV"));
  }

  @Test
  void testRoundTrip() {
    // Test round-trip conversion for integers
    for (int i = 0; i < 10000; i += 37) {
      String base32 = Base32Utils.toBase32(i);
      int result = Base32Utils.fromBase32(base32);
      assertEquals(i, result, "Round-trip failed for value: " + i);
    }

    // Test round-trip conversion for longs
    for (long i = 0L; i < 100000L; i += 1237L) {
      String base32 = Base32Utils.toBase32(i);
      long result = Base32Utils.fromBase32ToLong(base32);
      assertEquals(i, result, "Round-trip failed for value: " + i);
    }
  }
}
