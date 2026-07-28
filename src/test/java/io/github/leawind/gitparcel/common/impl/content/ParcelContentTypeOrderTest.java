package io.github.leawind.gitparcel.common.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParcelContentTypeOrderTest {
  @Test
  void loadsDependenciesFirstAndSavesThemLast() {
    var attachments = new TestType("attachments", Set.of());
    var blocks = new TestType("blocks", Set.of("attachments"));
    var entities = new TestType("entities", Set.of("attachments"));

    assertEquals(
        List.of(attachments, blocks, entities),
        ParcelContentTypeOrder.forLoad(List.of(entities, blocks, attachments)));
    assertEquals(
        List.of(entities, blocks, attachments),
        ParcelContentTypeOrder.forSave(List.of(entities, blocks, attachments)));
  }

  @Test
  void rejectsMissingAndCyclicDependencies() {
    assertThrows(
        IllegalStateException.class,
        () -> ParcelContentTypeOrder.forLoad(List.of(new TestType("blocks", Set.of("missing")))));
    assertThrows(
        IllegalStateException.class,
        () ->
            ParcelContentTypeOrder.forLoad(
                List.of(
                    new TestType("alpha", Set.of("beta")),
                    new TestType("beta", Set.of("alpha")))));
  }

  private record TestType(Spec spec, Set<String> loadAfter)
      implements ParcelContentType<ParcelContentConfig.None> {
    private TestType(String id, Set<String> loadAfter) {
      this(new Spec(id, 1), loadAfter);
    }

    @Override
    public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source) {}

    @Override
    public void load(LoadContext<ParcelContentConfig.None> context, ParcelDataSink sink) {}
  }
}
