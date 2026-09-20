package io.github.leawind.gitparcel.common.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ParcelSemanticsTest extends AbstractMinecraftTest {
  private static final Identifier PROCESSOR = Identifier.fromNamespaceAndPath("gitparcel", "core");

  @Test
  void roundTripsThroughJson() {
    var semantics =
        new ParcelSemantics(
            List.of(PROCESSOR),
            List.of(
                new ParcelSemantics.CoordinateField(
                    "ENTITY", "minecraft:cow", "home", "BLOCK_POS", null),
                new ParcelSemantics.CoordinateField("BLOCK_ENTITY", null, "Items[].tag.wp", "POSITION", null)),
            List.of(new ParcelSemantics.ReferenceField("minecraft:cow", "leash.UUID")),
            List.of(
                new ParcelSemantics.AttachmentType(
                    Identifier.fromNamespaceAndPath("gitparcel", "map"), 1)));

    var encoded = ParcelSemantics.CODEC.encodeStart(JsonOps.INSTANCE, semantics).result().orElseThrow();
    var decoded = ParcelSemantics.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

    assertEquals(semantics, decoded);
    assertTrue(decoded.declaresProcessor(PROCESSOR));
    assertFalse(decoded.declaresProcessor(Identifier.fromNamespaceAndPath("gitparcel", "other")));
  }

  @Test
  void declaresCoordinateFieldsByIdentity() {
    var recorded =
        new ParcelSemantics(
            List.of(),
            List.of(new ParcelSemantics.CoordinateField("ENTITY", "minecraft:cow", "home", "BLOCK_POS", null)),
            List.of(),
            List.of());
    var same =
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            Identifier.fromNamespaceAndPath("minecraft", "cow"),
            "home",
            ParcelCoordinateField.Encoding.BLOCK_POS);
    var otherPath =
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            Identifier.fromNamespaceAndPath("minecraft", "cow"),
            "sleeping_pos",
            ParcelCoordinateField.Encoding.BLOCK_POS);
    var anyType =
        ParcelCoordinateField.forAny(
            ParcelCoordinateField.Target.ENTITY, "home", ParcelCoordinateField.Encoding.BLOCK_POS);

    assertTrue(recorded.declares(same));
    assertFalse(recorded.declares(otherPath));
    assertFalse(recorded.declares(anyType), "a typed record must not match an any-type field");
  }

  @Test
  void declaresReferenceFieldsByIdentity() {
    var recorded =
        new ParcelSemantics(
            List.of(),
            List.of(),
            List.of(new ParcelSemantics.ReferenceField(null, "leash.UUID")),
            List.of());
    var any =
        ParcelEntityRefField.forAny("leash.UUID");
    var typed =
        ParcelEntityRefField.forType(
            Identifier.fromNamespaceAndPath("minecraft", "cow"), "leash.UUID");

    assertTrue(recorded.declares(any));
    assertFalse(recorded.declares(typed), "an any-type record must not match a typed field");
  }
}
