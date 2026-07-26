package io.github.leawind.gitparcel.server.minecraft.logic.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OperationManagerTest {
  @Test
  void runsActionReportsProgressAndPublishesTerminalSnapshot() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var completed = new CountDownLatch(1);
    var result = new AtomicReference<OperationSnapshot>();
    try (var manager = new OperationManager(Runnable::run, 1, 2)) {
      var submitted =
          manager.submit(
              "fetch",
              "example",
              "tester",
              progress -> {
                progress.report("objects", 2, 5, "objects");
                progress.report("objects", 1, 5, "objects");
                started.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return "updated";
              },
              snapshot -> {
                result.set(snapshot);
                completed.countDown();
              });

      assertTrue(
          submitted.state() == OperationSnapshot.State.QUEUED
              || submitted.state() == OperationSnapshot.State.RUNNING);
      assertTrue(started.await(5, TimeUnit.SECONDS));
      var running = manager.get(submitted.operationId()).orElseThrow();
      assertEquals(OperationSnapshot.State.RUNNING, running.state());
      assertEquals(2, running.completed());
      release.countDown();
      assertTrue(completed.await(5, TimeUnit.SECONDS));
      assertEquals(OperationSnapshot.State.SUCCEEDED, result.get().state());
      assertEquals("updated", result.get().result().orElseThrow());
    }
  }

  @Test
  void rejectsWorkWhenBoundedQueueIsFull() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var manager = new OperationManager(Runnable::run, 1, 1)) {
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
      manager.submit("pull", "second", "tester", () -> "done", ignored -> {});

      var rejected =
          manager.submit("pull", "third", "tester", () -> "done", ignored -> {});

      assertEquals(OperationSnapshot.State.FAILED, rejected.state());
      assertEquals("Operation queue is full", rejected.error().orElseThrow());
      release.countDown();
    }
  }

  @Test
  void shutdownCancelsOperationAndCompletesOnce() throws Exception {
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var completions = new AtomicInteger();
    var manager = new OperationManager(Runnable::run, 1, 1);
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
        OperationSnapshot.State.CANCELED,
        manager.get(operation.operationId()).orElseThrow().state());
    assertEquals(1, completions.get());
  }

  @Test
  void manualOperationHasExactlyOneTerminalState() {
    try (var manager = new OperationManager(Runnable::run, 1, 1);
        var operation = manager.begin("save_snapshot", "parcel", "tester")) {
      operation.progress().report("capture", 3, "sections");
      assertEquals(OperationSnapshot.State.SUCCEEDED, operation.succeed("snapshot").state());
      assertEquals(OperationSnapshot.State.SUCCEEDED, operation.fail(new Exception("late")).state());
    }
  }

  @Test
  void bridgesWorldPhaseToCallbackExecutor() throws Exception {
    var completed = new CountDownLatch(1);
    var result = new AtomicReference<OperationSnapshot>();
    try (var callbackExecutor =
            Executors.newSingleThreadExecutor(
                runnable -> new Thread(runnable, "test-server-thread"));
        var manager = new OperationManager(callbackExecutor, 1, 1)) {
      manager.submit(
          "save_snapshot",
          "parcel",
          "tester",
          ignored -> manager.call(() -> Thread.currentThread().getName()),
          snapshot -> {
            result.set(snapshot);
            completed.countDown();
          });

      assertTrue(completed.await(5, TimeUnit.SECONDS));
      assertEquals(OperationSnapshot.State.SUCCEEDED, result.get().state());
      assertEquals("test-server-thread", result.get().result().orElseThrow());
    }
  }
}
