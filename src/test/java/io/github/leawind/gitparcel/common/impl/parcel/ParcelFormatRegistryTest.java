package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import org.junit.jupiter.api.Test;

public class ParcelFormatRegistryTest extends AbstractGitParcelTest {
  @Test
  void keepsReadersAndWritersIndependent() {
    var registry = ParcelFormatRegistry.get();

    assertEquals(registry.defaultWriter(), registry.getWriter("alpha"));
    assertNotNull(registry.getWriter("beta"));
    assertNull(registry.getReader("beta"));
    assertNull(registry.getWriter("charlie"));
    assertNotNull(registry.getReader("charlie"));
    assertNull(registry.getWriter("non-existent"));
  }

  @Test
  void rejectsNegativeFormatVersions() {
    assertThrows(IllegalArgumentException.class, () -> new ParcelFormat.Spec("test", -1));
  }
}
