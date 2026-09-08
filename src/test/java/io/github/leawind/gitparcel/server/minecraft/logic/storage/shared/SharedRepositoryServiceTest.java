package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.SharedRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedRepositoryServiceTest {
  private static final GitRepositoryCore.Identity IDENTITY =
      new GitRepositoryCore.Identity("Tester", "tester@gitparcel.local");

  @TempDir Path tempDir;

  @Test
  void createsAndCatalogsLocalRepository() throws Exception {
    var service = new SharedRepositoryService(tempDir);

    service.create("builds");

    assertTrue(SharedRepository.get(tempDir.resolve("builds")).hasDotGit());
    assertEquals("local", service.list().get("builds").type());
    assertEquals(tempDir.resolve("builds"), service.repositoryPath("builds"));
    assertThrows(IOException.class, () -> service.create("builds"));
    assertTrue(SharedRepository.get(tempDir.resolve("builds")).hasDotGit());
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
    Path metadata = tempDir.resolve("builds/gitparcel.json");

    var absent = content.snapshotRepoMeta("builds");
    content.addParcelPath("builds", "spawn/house");
    content.restoreRepoMeta("builds", absent);
    assertFalse(Files.exists(metadata));

    Files.writeString(
        metadata, "{\n  \"schema_version\": 1,\n  \"parcels\": [\"old\"]\n}\n");
    byte[] expected = Files.readAllBytes(metadata);
    var existing = content.snapshotRepoMeta("builds");
    content.addParcelPath("builds", "new");
    content.restoreRepoMeta("builds", existing);
    assertEquals(List.of("old"), content.loadRepoMeta("builds"));
    assertArrayEquals(expected, Files.readAllBytes(metadata));
  }

  @Test
  void tryAcquireDoesNotWaitForBusyRepository() throws Exception {
    var service = new SharedRepositoryService(tempDir);
    service.create("builds");
    var acquired = new CountDownLatch(1);
    var release = new CountDownLatch(1);

    try (var executor = Executors.newSingleThreadExecutor()) {
      var holder =
          executor.submit(
              () -> {
                try (var ignored = service.acquire("builds")) {
                  acquired.countDown();
                  release.await();
                }
                return null;
              });
      try {
        assertTrue(acquired.await(5, TimeUnit.SECONDS));
        assertTrue(service.tryAcquire("builds").isEmpty());
      } finally {
        release.countDown();
      }
      holder.get(5, TimeUnit.SECONDS);
    }
    try (var lease = service.tryAcquire("builds").orElseThrow()) {
      assertEquals("builds", lease.name());
    }
  }

  @Test
  void validatesVersionedManifestAgainstTheSelectedCommitTree() throws Exception {
    var service = new SharedRepositoryService(tempDir);
    service.create("builds");
    Path repository = service.repositoryPath("builds");
    Files.createDirectories(repository.resolve("house/data"));
    Files.writeString(repository.resolve("house/parcel.json"), "{}");
    Files.writeString(repository.resolve("house/data/blocks.txt"), "stone");
    service.content().saveRepoMeta("builds", List.of("house"));
    var first =
        SharedRepository.get(repository)
            .commit(
                List.of("house", SharedContent.REPOSITORY_MANIFEST_FILE),
                "Add house",
                IDENTITY)
            .orElseThrow();

    assertEquals(List.of("house"), service.parcelPaths("builds", first.revision()));

    Files.createDirectories(repository.resolve("unlisted/data"));
    Files.writeString(repository.resolve("unlisted/parcel.json"), "{}");
    Files.writeString(repository.resolve("unlisted/data/blocks.txt"), "dirt");
    var inconsistent =
        SharedRepository.get(repository)
            .commit("unlisted", "Add unlisted parcel", IDENTITY)
            .orElseThrow();
    assertThrows(
        IOException.class,
        () -> service.parcelPaths("builds", inconsistent.revision()));
  }
}
