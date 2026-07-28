package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelContentTypeTest extends AbstractMinecraftTest {
  @Test
  void validatesSpecs() {
    assertTrue(ParcelContentType.Spec.ID_PATTERN.matcher("blocks").matches());
    assertTrue(ParcelContentType.Spec.ID_PATTERN.matcher("othermod.map_data").matches());
    assertFalse(ParcelContentType.Spec.ID_PATTERN.matcher("16_blocks").matches());
    assertThrows(
        IllegalArgumentException.class, () -> new ParcelContentType.Spec("16_blocks", 1));
    assertThrows(IllegalArgumentException.class, () -> new ParcelContentType.Spec("valid", -1));
  }

  @Test
  void contextsCarryOnlyPortableContentState() {
    var write =
        new ParcelContentType.SaveContext<ParcelContentConfig.None>(
            new Vec3i(3, 5, 7),
            new Vec3i(1, 2, 3),
            4444,
            Path.of("parcel-data"),
            null,
            ProgressReporter.NONE);
    var read =
        new ParcelContentType.LoadContext<ParcelContentConfig.None>(
            write.parcelSize(),
            write.anchor(),
            write.dataVersion(),
            write.directory(),
            write.config(),
            ProgressReporter.NONE);

    assertEquals(write.parcelSize(), read.parcelSize());
    assertEquals(write.anchor(), read.anchor());
    assertEquals(4444, read.dataVersion());
    assertEquals(Path.of("parcel-data"), read.directory());
  }
}
