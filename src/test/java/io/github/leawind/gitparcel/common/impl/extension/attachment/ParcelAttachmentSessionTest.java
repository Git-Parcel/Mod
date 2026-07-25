package io.github.leawind.gitparcel.common.impl.extension.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ParcelAttachmentSessionTest extends AbstractMinecraftTest {
  private static final Identifier TYPE =
      Identifier.fromNamespaceAndPath("test", "attachment");

  @Test
  void deduplicatesEqualSourceIdentitiesAndAssignsStableLocalIds() {
    var session = new ParcelAttachmentSession();
    var firstPayload = new CompoundTag();
    firstPayload.putString("value", "first");

    var first = session.collect(new String("same"), TYPE, 2, true, firstPayload);
    var duplicate =
        session.collect(new String("same"), TYPE, 2, true, new CompoundTag());
    var second = session.collect("different", TYPE, 2, false, new CompoundTag());
    var otherType =
        session.collect(
            new String("same"),
            Identifier.fromNamespaceAndPath("test", "other"),
            0,
            false,
            new CompoundTag());

    assertEquals("a00000000", first.value());
    assertEquals(first, duplicate);
    assertEquals("a00000001", second.value());
    assertEquals("a00000002", otherType.value());
    assertEquals(3, session.captured().size());
    assertEquals(
        "first",
        session
            .captured()
            .getFirst()
            .payload()
            .getString("value")
            .orElseThrow());
  }

  @Test
  void resolvesAttachmentsOnceAndByRuntimeType() {
    var session = new ParcelAttachmentSession();
    var id = session.collect("source", TYPE, 0, false, new CompoundTag());

    session.resolve(id, "resolved");

    assertEquals("resolved", session.findResolved(id, String.class).orElseThrow());
    assertEquals(java.util.Optional.empty(), session.findResolved(id, Integer.class));
    assertThrows(IllegalStateException.class, () -> session.resolve(id, "again"));
  }
}
