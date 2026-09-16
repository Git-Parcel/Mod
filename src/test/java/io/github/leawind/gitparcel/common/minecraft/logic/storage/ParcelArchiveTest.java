package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.InternalRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParcelArchiveTest extends AbstractGitParcelTest {
  private static final GitRepositoryCore.Identity PLAYER =
      new GitRepositoryCore.Identity("Player", "player@gitparcel.local");
  private static final GitRepositoryCore.Identity SERVER =
      new GitRepositoryCore.Identity("Server", "server@gitparcel.local");

  @TempDir Path tempDir;

  @Test
  void derivesImplicitDirectoryFromArchiveIdentity() throws IOException {
    UUID id = UUID.randomUUID();
    var archive = ParcelArchive.at(tempDir, id);

    assertEquals(id, archive.id());
    assertEquals(tempDir.toAbsolutePath().normalize().resolve(id + ".git"), archive.directory());
    assertFalse(archive.exists());
    assertEquals(0, archive.sizeBytes());
  }

  @Test
  void archiveOfParcelUsesParcelIdentity() {
    var parcel =
        ParcelFactory.create(new BoundingBox(0, 0, 0, 2, 2, 2), Mirror.NONE, Rotation.NONE);
    var archive = ParcelArchive.forParcel(parcel, tempDir);

    assertEquals(parcel.uuid(), archive.id());
    assertEquals(ParcelArchive.at(tempDir, parcel.uuid()).directory(), archive.directory());
  }

  @Test
  void readsSnapshotMetadataAndSyncState() throws Exception {
    var parcel =
        ParcelFactory.create(
            BoundingBox.fromCorners(new BlockPos(4, 64, 7), new BlockPos(9, 70, 12)),
            Mirror.NONE,
            Rotation.NONE);
    var archive = ParcelArchive.forParcel(parcel, tempDir);
    Path workspace = workspaceWithMetadata(parcel.meta());

    SnapshotId commit =
        archive
            .repository()
            .saveSnapshot(workspace, saveMetadata(), ProgressReporter.NONE);

    assertTrue(archive.exists());
    ParcelMeta meta = archive.readSnapshotMeta(commit);
    assertEquals(parcel.meta().size(), meta.size());
    assertEquals(parcel.meta().anchor(), meta.anchor());
    assertEquals(parcel.meta().dataVersion(), meta.dataVersion());

    Parcel.ArchiveSync sync = archive.readSyncState(commit);
    assertEquals(parcel.meta().size(), sync.size());
    assertEquals(parcel.meta().anchor(), sync.anchor());
    assertTrue(sync.repositorySizeBytes() > 0);
    assertEquals(archive.sizeBytes(), sync.repositorySizeBytes());
  }

  @Test
  void readSnapshotMetaRejectsUnknownSnapshots() {
    var archive = ParcelArchive.at(tempDir, UUID.randomUUID());
    assertThrows(
        IOException.class,
        () -> archive.readSnapshotMeta(new SnapshotId("0".repeat(40))));
  }

  private Path workspaceWithMetadata(ParcelMeta meta) throws Exception {
    Path root = Files.createTempDirectory(tempDir, "workspace-");
    Files.createDirectories(root.resolve("data"));
    var json =
        (JsonObject) ParcelMeta.CODEC.encodeStart(JsonOps.INSTANCE, meta).getOrThrow();
    Files.writeString(root.resolve("parcel.json"), new Gson().toJson(json));
    Files.writeString(root.resolve("data/content.txt"), "content");
    return root;
  }

  private static InternalRepository.SaveMetadata saveMetadata() {
    return new InternalRepository.SaveMetadata(
        "Snapshot", "Description", PLAYER, SERVER, SnapshotNode.Source.SAVED, Instant.now());
  }
}
