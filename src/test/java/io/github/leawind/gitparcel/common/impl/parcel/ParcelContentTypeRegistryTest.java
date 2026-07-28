package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelContentTypeRegistryTest {
  @Test
  void selectsTheNewestVersionPerDirectoryId() {
    var registry = new ParcelContentTypeRegistryImpl();
    var blocks1 = new TestContentType("blocks", 1);
    var entities1 = new TestContentType("entities", 1);
    var blocks3 = new TestContentType("blocks", 3);
    var blocks2 = new TestContentType("blocks", 2);

    registry.register(blocks1);
    registry.register(entities1);
    registry.register(blocks3);
    registry.register(blocks2);
    registry.freeze();

    assertSame(blocks3, registry.latest("blocks"));
    assertEquals(List.of(blocks3, entities1), registry.latestTypes());
    assertSame(blocks1, registry.get(blocks1.spec()));
    assertSame(blocks2, registry.get(blocks2.spec()));
  }

  @Test
  void rejectsDuplicateVersionsAndUnsafeDirectoryNames() {
    var registry = new ParcelContentTypeRegistryImpl();
    registry.register(new TestContentType("blocks", 1));

    assertThrows(
        IllegalArgumentException.class,
        () -> registry.register(new TestContentType("blocks", 1)));
    assertThrows(IllegalArgumentException.class, () -> new TestContentType("../blocks", 1));
    assertThrows(IllegalArgumentException.class, () -> new TestContentType("blocks", -1));
  }

  private static final class TestContentType
      implements ParcelContentType<ParcelContentConfig.None> {
    private final Spec spec;

    private TestContentType(String id, int version) {
      spec = new Spec(id, version);
    }

    @Override
    public Spec spec() {
      return spec;
    }

    @Override
    public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source) {}

    @Override
    public void load(LoadContext<ParcelContentConfig.None> context, ParcelDataSink sink) {}
  }
}
