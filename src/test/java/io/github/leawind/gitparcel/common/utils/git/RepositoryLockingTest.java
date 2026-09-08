package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards the unified per-path repository lock: every facade over one repository directory must
 * share the same lock, and read paths must fail fast when that lock is held.
 */
class RepositoryLockingTest {
  private static final GitRepositoryCore.Identity PLAYER =
      new GitRepositoryCore.Identity("Player", "player@gitparcel.local");
  private static final GitRepositoryCore.Identity SERVER =
      new GitRepositoryCore.Identity("Server", "server@gitparcel.local");

  @TempDir Path tempDir;

  @Test
  void readPathsShareTheCorePathLockAndFailFastWhenItIsHeld() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    repository.initialize();
    repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);

    var held = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var errors = new AtomicInteger();
    var holder =
        new Thread(
            () -> {
              try {
                // A bare core instance still maps to the same per-path lock entry.
                new GitRepositoryCore(repository.path(), RepositoryPolicy.INTERNAL)
                    .withLock(
                        () -> {
                          held.countDown();
                          try {
                            release.await(10, TimeUnit.SECONDS);
                          } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                          }
                          return null;
                        });
              } catch (Throwable e) {
                errors.incrementAndGet();
              }
            },
            "lock-holder");
    holder.start();
    assertTrue(held.await(5, TimeUnit.SECONDS));

    assertThrows(
        RepositoryBusyException.class,
        () -> repository.queryTree(UUID.randomUUID(), 10, Optional.empty()));

    release.countDown();
    holder.join(5000);
    assertEquals(0, errors.get());
  }

  @Test
  void concurrentSaveSequencesSerializeOnTheSharedPathLock() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    repository.initialize();

    var failures = new AtomicInteger();
    var ready = new CountDownLatch(2);
    var go = new CountDownLatch(1);
    var saver =
        new Thread(
            () -> {
              try {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                repository.saveSnapshot(workspace("thread"), metadata("Thread"), ProgressReporter.NONE);
              } catch (Throwable e) {
                failures.incrementAndGet();
              }
            },
            "saver");
    var mainPath = workspace("main");
    var other =
        new Thread(
            () -> {
              try {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                repository.saveSnapshot(mainPath, metadata("Main"), ProgressReporter.NONE);
              } catch (Throwable e) {
                failures.incrementAndGet();
              }
            },
            "other-saver");
    saver.start();
    other.start();
    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    saver.join(10_000);
    other.join(10_000);
    assertEquals(0, failures.get());

    var page = repository.queryTree(UUID.randomUUID(), 10, Optional.empty());
    assertEquals(2, page.nodes().size());
    long roots = page.nodes().stream().filter(node -> node.parentId().isEmpty()).count();
    assertEquals(1, roots);
    assertTrue(page.current().isPresent());
  }

  @Test
  void sharedFacadeRefusesInternalParcelRepositories() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> SharedRepository.get(repository.path()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SharedRepository.cloneRepository(
                "https://example.com/repo.git", repository.path(), null));
  }

  private Path workspace(String content) throws Exception {
    Path root = Files.createTempDirectory(tempDir, "workspace-");
    Files.createDirectories(root.resolve("data"));
    Files.writeString(root.resolve("parcel.json"), "{}");
    Files.writeString(root.resolve("data/content.txt"), content);
    return root;
  }

  private static InternalRepository.SaveMetadata metadata(String name) {
    return new InternalRepository.SaveMetadata(
        name, "Description", PLAYER, SERVER, SnapshotNode.Source.SAVED, Instant.now());
  }
}
