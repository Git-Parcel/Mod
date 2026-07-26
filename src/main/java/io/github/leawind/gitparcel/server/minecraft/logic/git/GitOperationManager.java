package io.github.leawind.gitparcel.server.minecraft.logic.git;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs potentially blocking Git work outside the Minecraft server thread. */
public final class GitOperationManager implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GitOperationManager.class);
  private static final int RETAINED_OPERATIONS = 100;
  private static final int QUEUE_CAPACITY = 32;

  private static final ConcurrentHashMap<MinecraftServer, GitOperationManager> INSTANCES =
      new ConcurrentHashMap<>();

  public static GitOperationManager get(MinecraftServer server) {
    return INSTANCES.computeIfAbsent(
        server,
        ignored -> new GitOperationManager(server::execute, 2, QUEUE_CAPACITY));
  }

  public static void shutdown(MinecraftServer server) {
    var manager = INSTANCES.remove(server);
    if (manager != null) {
      manager.close();
    }
  }

  private final Executor callbackExecutor;
  private final ThreadPoolExecutor executor;
  private final AtomicLong nextId = new AtomicLong(1);
  private final ConcurrentHashMap<Long, MutableOperation> operations =
      new ConcurrentHashMap<>();

  GitOperationManager(Executor callbackExecutor, int workerCount, int queueCapacity) {
    this.callbackExecutor = callbackExecutor;
    this.executor =
        new ThreadPoolExecutor(
            workerCount,
            workerCount,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new GitThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
  }

  public OperationSnapshot submit(
      String type,
      String repository,
      String requestedBy,
      Callable<String> action,
      Consumer<OperationSnapshot> completion) {
    long id = nextId.getAndIncrement();
    var operation =
        new MutableOperation(
            new OperationSnapshot(
                id,
                type,
                repository,
                requestedBy,
                Status.QUEUED,
                Instant.now(),
                null,
                null,
                "Queued"),
            completion);
    operations.put(id, operation);
    trimHistory();

    try {
      executor.execute(() -> execute(operation, action));
    } catch (RejectedExecutionException e) {
      var failed = operation.finish(Status.FAILED, "Git operation queue is full");
      dispatchCompletion(operation, failed);
    }
    return operation.snapshot();
  }

  public Optional<OperationSnapshot> get(long id) {
    var operation = operations.get(id);
    return operation == null ? Optional.empty() : Optional.of(operation.snapshot());
  }

  public List<OperationSnapshot> recent(int limit) {
    if (limit < 1) {
      throw new IllegalArgumentException("Operation limit must be positive");
    }
    return operations.values().stream()
        .map(MutableOperation::snapshot)
        .sorted(Comparator.comparingLong(OperationSnapshot::id).reversed())
        .limit(limit)
        .toList();
  }

  private void execute(MutableOperation operation, Callable<String> action) {
    if (!operation.start()) {
      return;
    }

    OperationSnapshot completed;
    try {
      String detail = action.call();
      completed = operation.finish(Status.SUCCEEDED, detail == null ? "Completed" : detail);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      completed = operation.finish(Status.CANCELLED, "Cancelled while stopping the server");
    } catch (Exception e) {
      LOGGER.error(
          "Git operation {} ({}) failed for repository {}",
          operation.snapshot().id(),
          operation.snapshot().type(),
          operation.snapshot().repository(),
          e);
      completed = operation.finish(Status.FAILED, describe(e));
    }
    dispatchCompletion(operation, completed);
  }

  private void dispatchCompletion(
      MutableOperation operation, OperationSnapshot completed) {
    if (!operation.claimCompletion()) {
      return;
    }
    try {
      callbackExecutor.execute(() -> operation.completion().accept(completed));
    } catch (RuntimeException e) {
      LOGGER.warn("Failed to dispatch completion for Git operation {}", completed.id(), e);
    }
  }

  private void trimHistory() {
    int excess = operations.size() - RETAINED_OPERATIONS;
    if (excess <= 0) {
      return;
    }
    operations.values().stream()
        .filter(operation -> operation.snapshot().status().isTerminal())
        .sorted(Comparator.comparingLong(operation -> operation.snapshot().id()))
        .limit(excess)
        .forEach(operation -> operations.remove(operation.snapshot().id(), operation));
  }

  @Override
  public void close() {
    executor.shutdownNow();
    for (MutableOperation operation : operations.values()) {
      var snapshot = operation.cancelIfPending();
      if (snapshot != null) {
        dispatchCompletion(operation, snapshot);
      }
    }
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null || message.isBlank() ? "" : ": " + message);
  }

  public enum Status {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
      return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
  }

  public record OperationSnapshot(
      long id,
      String type,
      String repository,
      String requestedBy,
      Status status,
      Instant submittedAt,
      @Nullable Instant startedAt,
      @Nullable Instant completedAt,
      String detail) {}

  private static final class MutableOperation {
    private volatile OperationSnapshot snapshot;
    private final Consumer<OperationSnapshot> completion;
    private boolean completionClaimed;

    private MutableOperation(
        OperationSnapshot snapshot, Consumer<OperationSnapshot> completion) {
      this.snapshot = snapshot;
      this.completion = completion;
    }

    private OperationSnapshot snapshot() {
      return snapshot;
    }

    private Consumer<OperationSnapshot> completion() {
      return completion;
    }

    private synchronized boolean claimCompletion() {
      if (completionClaimed) {
        return false;
      }
      completionClaimed = true;
      return true;
    }

    private synchronized boolean start() {
      if (snapshot.status() != Status.QUEUED) {
        return false;
      }
      snapshot =
          new OperationSnapshot(
              snapshot.id(),
              snapshot.type(),
              snapshot.repository(),
              snapshot.requestedBy(),
              Status.RUNNING,
              snapshot.submittedAt(),
              Instant.now(),
              null,
              "Running");
      return true;
    }

    private synchronized OperationSnapshot finish(Status status, String detail) {
      if (snapshot.status().isTerminal()) {
        return snapshot;
      }
      snapshot =
          new OperationSnapshot(
              snapshot.id(),
              snapshot.type(),
              snapshot.repository(),
              snapshot.requestedBy(),
              status,
              snapshot.submittedAt(),
              snapshot.startedAt(),
              Instant.now(),
              detail);
      return snapshot;
    }

    private synchronized @Nullable OperationSnapshot cancelIfPending() {
      if (snapshot.status().isTerminal()) {
        return null;
      }
      return finish(Status.CANCELLED, "Cancelled while stopping the server");
    }
  }

  private static final class GitThreadFactory implements ThreadFactory {
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Thread newThread(Runnable runnable) {
      var thread =
          new Thread(
              runnable,
              "Git Parcel Git IO-" + sequence.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }
}
