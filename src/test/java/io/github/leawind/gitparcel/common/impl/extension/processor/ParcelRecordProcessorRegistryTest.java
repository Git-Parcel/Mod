package io.github.leawind.gitparcel.common.impl.extension.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.extension.RegistrationSource;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ParcelRecordProcessorRegistryTest extends AbstractMinecraftTest {
  @Test
  void resolvesOrderingConstraintsDeterministically() {
    var registry = new ParcelRecordProcessorRegistryImpl();
    var middle = processor("middle", Set.of(id("first")), Set.of(id("last")));
    registry.register(guest("last"), processor("last"));
    registry.register(guest("middle"), middle);
    registry.register(guest("first"), processor("first"));

    registry.freeze();

    assertEquals(
        java.util.List.of(id("first"), id("middle"), id("last")),
        registry.orderedProcessors().stream().map(ParcelRecordProcessor::id).toList());
  }

  @Test
  void rejectsMissingDependencies() {
    var registry = new ParcelRecordProcessorRegistryImpl();
    registry.register(
        guest("dependent"), processor("dependent", Set.of(id("missing")), Set.of()));

    assertThrows(IllegalStateException.class, registry::freeze);
  }

  @Test
  void rejectsOrderingCycles() {
    var registry = new ParcelRecordProcessorRegistryImpl();
    registry.register(guest("one"), processor("one", Set.of(id("two")), Set.of()));
    registry.register(guest("two"), processor("two", Set.of(id("one")), Set.of()));

    assertThrows(IllegalStateException.class, registry::freeze);
  }

  /** Rule 7.4: the namespace owner wins no matter which registration came first. */
  @Test
  void ownersBeatGuestsRegardlessOfLoadOrder() {
    var registry = new ParcelRecordProcessorRegistryImpl();
    var guest = processor("leash");
    var owner = processor("leash");

    registry.register(new RegistrationSource("compat:extension", false, 0), guest);
    assertSame(guest, registry.get(id("leash")));
    registry.register(new RegistrationSource("leashed:core", true, 0), owner);
    assertSame(owner, registry.get(id("leash")));

    registry.register(new RegistrationSource("other:compat", false, 0), processor("leash"));
    assertSame(owner, registry.get(id("leash")));
    registry.freeze();
    assertEquals(1, registry.orderedProcessors().size());
  }

  /** Rule 7.4: guests adjudicate by explicit priority, then by extension id order. */
  @Test
  void guestsAdjudicateByPriorityThenExtensionId() {
    var registry = new ParcelRecordProcessorRegistryImpl();
    var low = processor("special");
    var high = processor("special");
    var tieCandidate = processor("special");
    var highest = processor("special");

    registry.register(new RegistrationSource("aaa:low", false, 0), low);
    registry.register(new RegistrationSource("zzz:high", false, 5), high);
    assertSame(high, registry.get(id("special")));

    // Equal priority falls back to the lexicographically smaller extension id.
    registry.register(new RegistrationSource("bbb:also-high", false, 5), tieCandidate);
    assertSame(tieCandidate, registry.get(id("special")));

    registry.register(new RegistrationSource("zzz:higher", false, 9), highest);
    assertSame(highest, registry.get(id("special")));
  }

  private static RegistrationSource guest(String extensionId) {
    return new RegistrationSource(extensionId, false, 0);
  }

  private static ParcelRecordProcessor processor(String path) {
    return processor(path, Set.of(), Set.of());
  }

  private static ParcelRecordProcessor processor(
      String path, Set<Identifier> runAfter, Set<Identifier> runBefore) {
    return new ParcelRecordProcessor() {
      @Override
      public Identifier id() {
        return ParcelRecordProcessorRegistryTest.id(path);
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
