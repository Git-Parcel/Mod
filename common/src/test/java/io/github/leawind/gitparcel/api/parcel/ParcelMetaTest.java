package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ParcelMetaTest {
  @Test
  void testNamePattern() {
    assertTrue("House".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("火柴盒".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("With space".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("Steve's home".matches(ParcelMeta.NAME_PATTERN.pattern()));

    assertFalse("Consecutive  spaces".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertFalse("Invalid\nchar".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertFalse("Invalid\rchar".matches(ParcelMeta.NAME_PATTERN.pattern()));
  }
}
