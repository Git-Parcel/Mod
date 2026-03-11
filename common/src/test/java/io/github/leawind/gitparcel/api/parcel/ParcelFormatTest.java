package io.github.leawind.gitparcel.api.parcel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class ParcelFormatTest {
  Predicate<String> idValidator = ParcelFormat.Info.ID_PATTERN.asPredicate();

  @Test
  void testIdPattern() {
    assertTrue(idValidator.test("mvp"));
    assertTrue(idValidator.test("parcella"));

    assertTrue(idValidator.test("parcella_d32"));
    assertTrue(idValidator.test("parcella_d16"));

    assertTrue(idValidator.test("parcella_d32_RLE3D"));
    assertTrue(idValidator.test("parcella_d32_FLAT"));

    assertFalse(idValidator.test("16_parcella"));
    assertFalse(idValidator.test("16_parcella"));
  }
}
