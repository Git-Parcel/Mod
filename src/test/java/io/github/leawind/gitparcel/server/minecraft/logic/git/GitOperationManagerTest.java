package io.github.leawind.gitparcel.server.minecraft.logic.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GitOperationManagerTest {
  @Test
  void runsActionAndPublishesTerminalSnapshot() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var completed = new CountDownLatch(1);
    var result = new AtomicReference<GitOperationManager.OperationSnapshot>();
    try (var manager = new GitOperationManager(Runnable::run, 1, 2)) {
      var submitted =
          manager.submit(
              "fetch",
              "example",
              "tester",
              () -> {
                started.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return "updated";
              },
              snapshot -> {
                result.set(snapshot);
                completed.countDown();
              });

      assertTrue(
          submitted.status() == GitOperationManager.Status.QUEUED
              || submitted.status() == GitOperationManager.Status.RUNNING);
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertEquals(
          GitOperationManager.Status.RUNNING,
          manager.get(submitted.id()).orElseThrow().status());
      release.countDown();
      assertTrue(completed.await(5, TimeUnit.SECONDS));
      assertEquals(GitOperationManager.Status.SUCCEEDED, result.get().status());
      assertEquals("updated", result.get().detail());
    }
  }

  @Test
  void rejectsWorkWhenBoundedQueueIsFull() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var manager = new GitOperationManager(Runnable::run, 1, 1)) {
      manager.submit(
          "pull",
          "first",
          "tester",
          () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            return "done";
          },
          ignored -> {});
      assertTrue(started.await(5, TimeUnit.SECONDS));
      manager.submit(
          "pull", "second", "tester", () -> "done", ignored -> {});

      var rejected =
          manager.submit(
              "pull", "third", "tester", () -> "done", ignored -> {});

      assertEquals(GitOperationManager.Status.FAILED, rejected.status());
      assertEquals("Git operation queue is full", rejected.detail());
      release.countDown();
    }
  }

  @Test
  void shutdownCancelsOperationAndCompletesOnce() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var completions = new AtomicInteger();
    var manager = new GitOperationManager(Runnable::run, 1, 1);
    var operation =
        manager.submit(
            "clone",
            "example",
            "tester",
            () -> {
              started.countDown();
              release.await(5, TimeUnit.SECONDS);
              return "done";
            },
            ignored -> completions.incrementAndGet());
    assertTrue(started.await(5, TimeUnit.SECONDS));

    manager.close();
    release.countDown();

    assertEquals(
        GitOperationManager.Status.CANCELLED,
        manager.get(operation.id()).orElseThrow().status());
    assertEquals(1, completions.get());
  }
}
