package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelMetaTest {
  @Test
  void testNamePattern() {
    assertTrue("House".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("火柴盒".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("With space".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertTrue("Steve's home".matches(ParcelMeta.NAME_PATTERN.pattern()));

    assertFalse("Consecutive  spaces".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertFalse("Invalid\nchar".matches(ParcelMeta.NAME_PATTERN.pattern()));
    assertFalse("Invalid\rchar".matches(ParcelMeta.NAME_PATTERN.pattern()));
  }

  @Test
  void roundTripsExplicitDataVersion() {
    var original =
        new ParcelMeta(
            new ParcelFormat.Spec("test", 0),
            4321,
            new Vec3i(3, 5, 7),
            new Vec3i(1, 2, 3));

    var encoded = ParcelMeta.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
    var decoded = ParcelMeta.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

    assertEquals(4321, encoded.getAsJsonObject().get("dataVersion").getAsInt());
    assertEquals(original.formatSpec(), decoded.formatSpec());
    assertEquals(original.dataVersion(), decoded.dataVersion());
    assertEquals(original.size(), decoded.size());
    assertEquals(original.anchor(), decoded.anchor());
  }
}
