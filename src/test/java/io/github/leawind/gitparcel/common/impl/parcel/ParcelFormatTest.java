package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Path;
import java.util.function.Predicate;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelFormatTest extends AbstractMinecraftTest {
  private final Predicate<String> idValidator = ParcelFormat.Spec.ID_PATTERN.asPredicate();

  @Test
  void validatesSpecs() {
    assertTrue(idValidator.test("parcella_d32"));
    assertTrue(idValidator.test("parcella_d16"));
    assertFalse(idValidator.test("16_parcella"));
    assertThrows(IllegalArgumentException.class, () -> new ParcelFormat.Spec("valid", -1));
  }

  @Test
  void contextsCarryOnlyPortableFormatState() {
    var write =
        new ParcelFormat.WriteContext<ParcelFormatConfig.None>(
            new Vec3i(3, 5, 7), new Vec3i(1, 2, 3), 4444, Path.of("parcel-data"), null);
    var read =
        new ParcelFormat.ReadContext<ParcelFormatConfig.None>(
            write.parcelSize(),
            write.anchor(),
            write.dataVersion(),
            write.dataDir(),
            write.config());

    assertEquals(write.parcelSize(), read.parcelSize());
    assertEquals(write.anchor(), read.anchor());
    assertEquals(4444, read.dataVersion());
    assertEquals(Path.of("parcel-data"), read.dataDir());
  }
}
