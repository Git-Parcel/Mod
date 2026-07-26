package io.github.leawind.gitparcel.server.minecraft.logic.operation;

import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.operation.ServerThreadBridge;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unified bounded execution and progress registry for parcel and shared-repository operations. */
public final class OperationManager implements AutoCloseable, ServerThreadBridge {
  private static final Logger LOGGER = LoggerFactory.getLogger(OperationManager.class);
  private static final int RETAINED_OPERATIONS = 100;
  private static final int QUEUE_CAPACITY = 32;
  private static final ConcurrentHashMap<MinecraftServer, OperationManager> INSTANCES =
      new ConcurrentHashMap<>();

  public static OperationManager get(MinecraftServer server) {
    return INSTANCES.computeIfAbsent(
        server, ignored -> new OperationManager(server::execute, 2, QUEUE_CAPACITY));
  }

  public static void shutdown(MinecraftServer server) {
    var manager = INSTANCES.remove(server);
    if (manager != null) {
      manager.close();
    }
  }

  private final Executor callbackExecutor;
  private final ThreadPoolExecutor executor;
  private final ConcurrentHashMap<UUID, MutableOperation> operations = new ConcurrentHashMap<>();

  public OperationManager(Executor callbackExecutor, int workerCount, int queueCapacity) {
    this.callbackExecutor = callbackExecutor;
    this.executor =
        new ThreadPoolExecutor(
            workerCount,
            workerCount,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new OperationThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
  }

  public OperationSnapshot submit(
      String kind,
      String target,
      String owner,
      OperationAction action,
      Consumer<OperationSnapshot> completion) {
    MutableOperation operation = create(kind, target, owner, completion, false);
    try {
      executor.execute(() -> execute(operation, action));
    } catch (RejectedExecutionException e) {
      var failed = operation.finish(OperationSnapshot.State.FAILED, null, "Operation queue is full");
      dispatchCompletion(operation, failed);
    }
    return operation.snapshot();
  }

  /** Convenience overload for remote Git actions that do not report progress. */
  public OperationSnapshot submit(
      String kind,
      String target,
      String owner,
      Callable<String> action,
      Consumer<OperationSnapshot> completion) {
    return submit(kind, target, owner, ignored -> action.call(), completion);
  }

  /** Registers work that must execute synchronously on the Minecraft server thread. */
  public OperationHandle begin(String kind, String target, String owner) {
    MutableOperation operation = create(kind, target, owner, ignored -> {}, true);
    return new OperationHandle(operation);
  }

  public Optional<OperationSnapshot> get(UUID id) {
    var operation = operations.get(id);
    return operation == null ? Optional.empty() : Optional.of(operation.snapshot());
  }

  /** Runs a world-access phase on the callback/server executor and waits on the worker thread. */
  @Override
  public <T> T call(Callable<T> action) throws Exception {
    var result = new CompletableFuture<T>();
    try {
      callbackExecutor.execute(
          () -> {
            try {
              result.complete(action.call());
            } catch (Throwable failure) {
              result.completeExceptionally(failure);
            }
          });
    } catch (RuntimeException e) {
      throw new RejectedExecutionException("Server callback was rejected", e);
    }
    boolean interrupted = false;
    for (; ; ) {
      try {
        T value = result.get();
        if (interrupted) {
          Thread.currentThread().interrupt();
          throw new InterruptedException("Interrupted while a server-thread phase completed");
        }
        return value;
      } catch (InterruptedException e) {
        // A world callback may already be mutating the level. Wait for its terminal result before
        // allowing the worker to close its workspace or unwind the durable restore record.
        interrupted = true;
      } catch (ExecutionException e) {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
        Throwable cause = e.getCause();
        if (cause instanceof Exception exception) {
          throw exception;
        }
        if (cause instanceof Error error) {
          throw error;
        }
        throw new RuntimeException(cause);
      }
    }
  }

  public List<OperationSnapshot> recent(int limit) {
    if (limit < 1) {
      throw new IllegalArgumentException("Operation limit must be positive");
    }
    return operations.values().stream()
        .map(MutableOperation::snapshot)
        .sorted(
            Comparator.comparing(OperationSnapshot::submittedAt)
                .thenComparing(snapshot -> snapshot.operationId().toString())
                .reversed())
        .limit(limit)
        .toList();
  }

  private MutableOperation create(
      String kind,
      String target,
      String owner,
      Consumer<OperationSnapshot> completion,
      boolean start) {
    Instant now = Instant.now();
    var snapshot =
        new OperationSnapshot(
            UUID.randomUUID(),
            requireText(kind, "Operation kind"),
            requireText(owner, "Operation owner"),
            requireText(target, "Operation target"),
            OperationSnapshot.State.QUEUED,
            "queued",
            0,
            Optional.empty(),
            Optional.empty(),
            now.toString(),
            Optional.empty(),
            now.toString(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    var operation = new MutableOperation(snapshot, completion);
    operations.put(snapshot.operationId(), operation);
    trimHistory();
    if (start) {
      operation.start();
    }
    return operation;
  }

  private void execute(MutableOperation operation, OperationAction action) {
    if (!operation.start()) {
      return;
    }
    OperationSnapshot completed;
    try {
      String result = action.run(operation.reporter());
      completed = operation.finish(OperationSnapshot.State.SUCCEEDED, result, null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      completed = operation.finish(OperationSnapshot.State.CANCELED, null, "Canceled while stopping server");
    } catch (Exception e) {
      LOGGER.error(
          "Operation {} ({}) failed for {}",
          operation.snapshot().operationId(),
          operation.snapshot().kind(),
          operation.snapshot().target(),
          e);
      completed = operation.finish(OperationSnapshot.State.FAILED, null, describe(e));
    }
    dispatchCompletion(operation, completed);
  }

  private void dispatchCompletion(MutableOperation operation, OperationSnapshot completed) {
    if (!operation.claimCompletion()) {
      return;
    }
    try {
      callbackExecutor.execute(() -> operation.completion().accept(completed));
    } catch (RuntimeException e) {
      LOGGER.warn("Failed to dispatch completion for operation {}", completed.operationId(), e);
    }
  }

  private void trimHistory() {
    int excess = operations.size() - RETAINED_OPERATIONS;
    if (excess <= 0) {
      return;
    }
    operations.values().stream()
        .filter(operation -> operation.snapshot().state().isTerminal())
        .sorted(Comparator.comparing(operation -> operation.snapshot().submittedAt()))
        .limit(excess)
        .forEach(operation -> operations.remove(operation.snapshot().operationId(), operation));
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

  private static String requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null || message.isBlank() ? "" : ": " + message);
  }

  @FunctionalInterface
  public interface OperationAction {
    String run(ProgressReporter progress) throws Exception;
  }

  public final class OperationHandle implements AutoCloseable {
    private final MutableOperation operation;
    private boolean terminal;

    private OperationHandle(MutableOperation operation) {
      this.operation = operation;
    }

    public UUID operationId() {
      return operation.snapshot().operationId();
    }

    public ProgressReporter progress() {
      return operation.reporter();
    }

    public OperationSnapshot snapshot() {
      return operation.snapshot();
    }

    public OperationSnapshot succeed(String result) {
      terminal = true;
      return operation.finish(OperationSnapshot.State.SUCCEEDED, result, null);
    }

    public OperationSnapshot fail(Exception exception) {
      terminal = true;
      return operation.finish(OperationSnapshot.State.FAILED, null, describe(exception));
    }

    @Override
    public void close() {
      if (!terminal) {
        fail(new IllegalStateException("Operation ended without a result"));
      }
    }
  }

  private static final class MutableOperation {
    private volatile OperationSnapshot snapshot;
    private final Consumer<OperationSnapshot> completion;
    private final ProgressReporter reporter = this::report;
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

    private ProgressReporter reporter() {
      return reporter;
    }

    private synchronized boolean claimCompletion() {
      if (completionClaimed) {
        return false;
      }
      completionClaimed = true;
      return true;
    }

    private synchronized boolean start() {
      if (snapshot.state() != OperationSnapshot.State.QUEUED) {
        return false;
      }
      Instant now = Instant.now();
      snapshot =
          copy(
              OperationSnapshot.State.RUNNING,
              "starting",
              0,
              Optional.empty(),
              Optional.empty(),
              Optional.of(now.toString()),
              now,
              Optional.empty(),
              Optional.empty(),
              Optional.empty());
      return true;
    }

    private synchronized void report(ProgressReporter.Update update) {
      if (snapshot.state() != OperationSnapshot.State.RUNNING) {
        return;
      }
      if (snapshot.phase().equals(update.phase()) && update.completed() < snapshot.completed()) {
        return;
      }
      Optional<Long> total =
          update.total().isPresent()
              ? Optional.of(update.total().getAsLong())
              : Optional.empty();
      snapshot =
          copy(
              snapshot.state(),
              update.phase(),
              update.completed(),
              total,
              update.unit().isBlank() ? Optional.empty() : Optional.of(update.unit()),
              snapshot.startedAt(),
              Instant.now(),
              snapshot.completedAt(),
              snapshot.result(),
              snapshot.error());
    }

    private synchronized OperationSnapshot finish(
        OperationSnapshot.State state, String result, String error) {
      if (snapshot.state().isTerminal()) {
        return snapshot;
      }
      Instant now = Instant.now();
      snapshot =
          copy(
              state,
              state.name().toLowerCase(),
              snapshot.completed(),
              snapshot.total(),
              snapshot.unit(),
              snapshot.startedAt(),
              now,
              Optional.of(now.toString()),
              Optional.ofNullable(result),
              Optional.ofNullable(error));
      return snapshot;
    }

    private synchronized OperationSnapshot cancelIfPending() {
      if (snapshot.state().isTerminal()) {
        return null;
      }
      return finish(OperationSnapshot.State.CANCELED, null, "Canceled while stopping server");
    }

    private OperationSnapshot copy(
        OperationSnapshot.State state,
        String phase,
        long completed,
        Optional<Long> total,
        Optional<String> unit,
        Optional<String> startedAt,
        Instant updatedAt,
        Optional<String> completedAt,
        Optional<String> result,
        Optional<String> error) {
      return new OperationSnapshot(
          snapshot.operationId(),
          snapshot.kind(),
          snapshot.owner(),
          snapshot.target(),
          state,
          phase,
          completed,
          total,
          unit,
          snapshot.submittedAt(),
          startedAt,
          updatedAt.toString(),
          completedAt,
          result,
          error);
    }
  }

  private static final class OperationThreadFactory implements ThreadFactory {
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public Thread newThread(Runnable runnable) {
      var thread = new Thread(runnable, "Git Parcel Operation-" + sequence.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    }
  }
}
