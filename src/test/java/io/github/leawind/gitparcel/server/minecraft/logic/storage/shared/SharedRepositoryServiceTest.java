package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedRepositoryServiceTest {
  @TempDir Path tempDir;

  @Test
  void createsAndCatalogsLocalRepository() throws Exception {
    var service = new SharedRepositoryService(tempDir);

    service.create("builds");

    assertTrue(GitRepo.get(tempDir.resolve("builds")).hasDotGit());
    assertEquals("local", service.list().get("builds").type());
    assertEquals(tempDir.resolve("builds"), service.repositoryPath("builds"));
    assertThrows(IOException.class, () -> service.create("builds"));
    assertTrue(GitRepo.get(tempDir.resolve("builds")).hasDotGit());
  }

  @Test
  void refusesToClaimPreexistingDirectory() throws Exception {
    Path existing = tempDir.resolve("existing");
    Files.createDirectories(existing);
    Files.writeString(existing.resolve("keep.txt"), "keep");
    var service = new SharedRepositoryService(tempDir);

    assertThrows(IOException.class, () -> service.create("existing"));
    assertEquals("keep", Files.readString(existing.resolve("keep.txt")));
    assertTrue(service.list().isEmpty());
  }

  @Test
  void restoresRepositoryMetadataExactlyAfterFailedOperation() throws Exception {
    var service = new SharedRepositoryService(tempDir);
    service.create("builds");
    var content = service.content();
    Path metadata = tempDir.resolve("builds/meta.json");

    var absent = content.snapshotRepoMeta("builds");
    content.addParcelPath("builds", "spawn/house");
    content.restoreRepoMeta("builds", absent);
    assertFalse(Files.exists(metadata));

    Files.writeString(metadata, "{\n  \"parcels\": [\"old\"]\n}\n");
    byte[] expected = Files.readAllBytes(metadata);
    var existing = content.snapshotRepoMeta("builds");
    content.addParcelPath("builds", "new");
    content.restoreRepoMeta("builds", existing);
    assertEquals(List.of("old"), content.loadRepoMeta("builds"));
    assertArrayEquals(expected, Files.readAllBytes(metadata));
  }
}
