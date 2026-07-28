package io.github.leawind.gitparcel.common.api.parcel.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelContentCapabilitiesTest extends AbstractGitParcelTest {
  @Test
  void createsCapabilitiesFromRegistry() {
    var capabilities = ParcelContentCapabilities.from(ParcelContentTypeRegistry.get());

    assertTrue(capabilities.isActive(new ParcelContentType.Spec("alpha", 0)));
    assertTrue(capabilities.isActive(new ParcelContentType.Spec("beta", 0)));
    assertTrue(capabilities.isActive(new ParcelContentType.Spec("charlie", 0)));
    assertEquals(3, capabilities.registered().size());
  }

  @Test
  void ownsImmutableCopies() {
    var registered = new ArrayList<>(List.of(new ParcelContentType.Spec("alpha", 0)));
    var capabilities = new ParcelContentCapabilities(registered, registered);

    registered.clear();

    assertEquals(1, capabilities.registered().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> capabilities.registered().add(new ParcelContentType.Spec("beta", 0)));
  }

  @Test
  void codecRoundTrips() {
    var expected = ParcelContentCapabilities.from(ParcelContentTypeRegistry.get());
    var json = ParcelContentCapabilities.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();

    var actual = ParcelContentCapabilities.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(expected, actual);
  }
}
