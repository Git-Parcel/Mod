package io.github.leawind.gitparcel.common.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelFormatCapabilitiesTest extends AbstractGitParcelTest {
  @Test
  void createsCapabilitiesFromRegistry() {
    var capabilities = ParcelFormatCapabilities.from(ParcelFormatRegistry.get());

    assertTrue(capabilities.hasSaver(new ParcelFormat.Spec("alpha", 0)));
    assertTrue(capabilities.hasSaver(new ParcelFormat.Spec("beta", 0)));
    assertTrue(capabilities.hasLoader(new ParcelFormat.Spec("charlie", 0)));
    assertFalse(capabilities.hasLoader(new ParcelFormat.Spec("beta", 0)));
    assertEquals(3, capabilities.toSet().size());
  }

  @Test
  void ownsImmutableCopies() {
    var savers = new ArrayList<>(List.of(new ParcelFormat.Spec("alpha", 0)));
    var capabilities = new ParcelFormatCapabilities(savers, List.of());

    savers.clear();

    assertEquals(1, capabilities.savers().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> capabilities.savers().add(new ParcelFormat.Spec("beta", 0)));
  }

  @Test
  void codecRoundTrips() {
    var expected = ParcelFormatCapabilities.from(ParcelFormatRegistry.get());
    var json = ParcelFormatCapabilities.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();

    var actual = ParcelFormatCapabilities.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(expected, actual);
  }
}
