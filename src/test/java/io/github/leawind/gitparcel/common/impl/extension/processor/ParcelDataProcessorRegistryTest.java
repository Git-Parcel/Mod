package io.github.leawind.gitparcel.common.impl.extension.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessor;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ParcelDataProcessorRegistryTest extends AbstractMinecraftTest {
  @Test
  void resolvesOrderingConstraintsDeterministically() {
    var registry = new ParcelDataProcessorRegistryImpl();
    var middle = processor("middle", Set.of(id("first")), Set.of(id("last")));
    registry.register(processor("last"));
    registry.register(middle);
    registry.register(processor("first"));

    registry.freeze();

    assertEquals(
        java.util.List.of(id("first"), id("middle"), id("last")),
        registry.orderedProcessors().stream().map(ParcelDataProcessor::id).toList());
  }

  @Test
  void rejectsMissingDependencies() {
    var registry = new ParcelDataProcessorRegistryImpl();
    registry.register(processor("dependent", Set.of(id("missing")), Set.of()));

    assertThrows(IllegalStateException.class, registry::freeze);
  }

  @Test
  void rejectsOrderingCycles() {
    var registry = new ParcelDataProcessorRegistryImpl();
    registry.register(processor("one", Set.of(id("two")), Set.of()));
    registry.register(processor("two", Set.of(id("one")), Set.of()));

    assertThrows(IllegalStateException.class, registry::freeze);
  }

  private static ParcelDataProcessor processor(String path) {
    return processor(path, Set.of(), Set.of());
  }

  private static ParcelDataProcessor processor(
      String path, Set<Identifier> runAfter, Set<Identifier> runBefore) {
    return new ParcelDataProcessor() {
      @Override
      public Identifier id() {
        return ParcelDataProcessorRegistryTest.id(path);
      }

      @Override
      public Set<Identifier> runAfter() {
        return runAfter;
      }

      @Override
      public Set<Identifier> runBefore() {
        return runBefore;
      }
    };
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath("test", path);
  }
}
