package io.github.leawind.gitparcel.common.impl.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import java.util.List;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class ParcelValidatorTest extends AbstractGitParcelTest {
  private static final long DEFAULT_LIMIT = 128L * 128L * 128L;

  @Test
  void acceptsNonOverlappingParcelWithinVolumeLimit() {
    var existing = createParcel(new BoundingBox(0, 0, 0, 9, 9, 9));
    var candidate = createParcel(new BoundingBox(10, 0, 0, 19, 9, 9));

    assertDoesNotThrow(
        () -> ParcelValidator.validateNewParcel(candidate, List.of(existing), DEFAULT_LIMIT));
  }

  @Test
  void rejectsDuplicateUuid() {
    var parcel = createParcel(new BoundingBox(0, 0, 0, 9, 9, 9));

    assertThrows(
        IllegalArgumentException.class,
        () -> ParcelValidator.validateNewParcel(parcel, List.of(parcel), DEFAULT_LIMIT));
  }

  @Test
  void rejectsOverlappingParcel() {
    var existing = createParcel(new BoundingBox(0, 0, 0, 9, 9, 9));
    var candidate = createParcel(new BoundingBox(9, 0, 0, 18, 9, 9));

    assertThrows(
        IllegalArgumentException.class,
        () -> ParcelValidator.validateNewParcel(candidate, List.of(existing), DEFAULT_LIMIT));
  }

  @Test
  void calculatesLargeVolumesWithoutIntegerOverflow() {
    var parcel = createParcel(new BoundingBox(0, 0, 0, 49_999, 49_999, 49_999));

    assertThrows(
        IllegalArgumentException.class,
        () -> ParcelValidator.validateNewParcel(parcel, List.of(), 1_000_000_000_000L));
  }

  private static Parcel createParcel(BoundingBox boundingBox) {
    return Parcel.create(boundingBox, Mirror.NONE, Rotation.NONE);
  }
}
