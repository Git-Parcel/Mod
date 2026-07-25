package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParcelRepositoryServiceTest extends AbstractGitParcelTest {
  private static final GitRepo.CommitIdentity IDENTITY =
      new GitRepo.CommitIdentity("Test Player", "gitparcel@localhost");

  @TempDir Path tempDir;

  @Test
  void requiresSaveBeforeCommitAndThenExposesHistory() throws Exception {
    var parcel =
        ParcelFactory.create(
            new BoundingBox(0, 0, 0, 1, 1, 1),
            Mirror.NONE,
            Rotation.NONE);

    assertThrows(
        ParcelException.class,
        () ->
            ParcelRepositoryService.commit(
                parcel, tempDir, "Before save", IDENTITY));

    var location = ParcelStorage.resolveRepositoryLocation(parcel, tempDir);
    Files.createDirectories(location.parcelDirectory());
    Files.writeString(
        location.parcelDirectory().resolve("parcel.json"), "{}");

    var commit =
        ParcelRepositoryService.commit(
                parcel, tempDir, "Initial snapshot", IDENTITY)
            .orElseThrow();
    var history = ParcelRepositoryService.history(parcel, tempDir, 10);

    assertEquals(1, history.size());
    assertEquals(commit.revision(), history.getFirst().revision());
    assertEquals("Initial snapshot", history.getFirst().message());
  }

  @Test
  void restoreGeometryMustMatchRegisteredParcel() {
    var spec = new ParcelFormat.Spec("test", 0);
    var current =
        new ParcelMeta(spec, 1, new Vec3i(3, 4, 5), new Vec3i(1, 0, 2));
    var matching =
        new ParcelMeta(spec, 2, new Vec3i(3, 4, 5), new Vec3i(1, 0, 2));
    var differentSize =
        new ParcelMeta(spec, 1, new Vec3i(4, 4, 5), new Vec3i(1, 0, 2));
    var differentAnchor =
        new ParcelMeta(spec, 1, new Vec3i(3, 4, 5), new Vec3i(0, 0, 0));

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
