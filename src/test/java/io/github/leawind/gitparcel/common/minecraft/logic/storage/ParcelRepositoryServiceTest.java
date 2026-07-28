package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import net.minecraft.core.Vec3i;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParcelRepositoryServiceTest extends AbstractGitParcelTest {
  @Test
  void restoreGeometryMustMatchRegisteredParcel() {
    var current =
        new ParcelMeta(Map.of(), 1, new Vec3i(3, 4, 5), new Vec3i(1, 0, 2));
    var matching =
        new ParcelMeta(Map.of(), 2, new Vec3i(3, 4, 5), new Vec3i(1, 0, 2));
    var differentSize =
        new ParcelMeta(Map.of(), 1, new Vec3i(4, 4, 5), new Vec3i(1, 0, 2));
    var differentAnchor =
        new ParcelMeta(Map.of(), 1, new Vec3i(3, 4, 5), new Vec3i(0, 0, 0));

    assertDoesNotThrow(
        () -> ParcelRepositoryService.validateGeometry(current, matching));
    assertThrows(
        ParcelException.class,
        () ->
            ParcelRepositoryService.validateGeometry(current, differentSize));
    assertThrows(
        ParcelException.class,
        () ->
            ParcelRepositoryService.validateGeometry(current, differentAnchor));
  }
}
