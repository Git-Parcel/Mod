package io.github.leawind.gitparcel.common.utils.git;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotWorkspaceFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.jgit.lib.RefUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A parcel-owned bare repository enforcing the immutable, single-parent snapshot tree. */
public final class InternalRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger(InternalRepository.class);
  public static final String CURRENT_REF = "refs/gitparcel/current";
  public static final String SNAPSHOTS_PREFIX = "refs/gitparcel/snapshots/";
  public static final String OPERATIONS_PREFIX = "refs/gitparcel/operations/";

  private static final String OPERATION_FILE = "operation.json";
  /** Machine-readable snapshot metadata travels as trailing {@code Git-Parcel-*: value} lines. */
  private static final String TRAILER_PREFIX = "Git-Parcel-";
  private static final Gson GSON = new Gson();

  /**
   * Read paths acquire the repository lock with a bounded timeout. A background save or restore
   * can hold the lock for a long time while waiting on the server thread, so an unbounded wait
   * here could freeze a server tick forever if a caller ignores the threading contract.
   */
  private static final long READ_LOCK_TIMEOUT_SECONDS = 5;

  public static InternalRepository at(Path parcelsRoot, UUID parcelUuid) {
    return new InternalRepository(parcelsRoot.resolve(parcelUuid + ".git"));
  }

  private final Path path;
  private final GitRepositoryCore core;
  private final SnapshotTreeLimits limits;

  public InternalRepository(Path path) {
    this(path, SnapshotTreeLimits.DEFAULT);
  }

  public InternalRepository(Path path, SnapshotTreeLimits limits) {
    this.path = path.toAbsolutePath().normalize();
    this.core = new GitRepositoryCore(this.path, RepositoryPolicy.INTERNAL);
    this.limits = limits;
    // Keep the shared-repository facade away from this directory tree.
    InternalParcelRoots.register(this.path.getParent());
  }

  public Path path() {
    return path;
  }

  public boolean exists() {
    return core.exists();
  }

  public void initialize() throws IOException {
    sequence(
        () -> {
          try {
            core.initializeBare();
          } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
            throw new IOException("Failed to initialize internal repository", e);
          }
          RefUpdate.Result result = core.linkHead(CURRENT_REF);
          if (!successful(result)) {
            throw new IOException("Failed to link repository HEAD to current snapshot: " + result);
          }
          return null;
        });
  }

  public RepositoryState inspect() {
    return inspect(true);
  }

  private RepositoryState inspect(boolean validateCompleteTrees) {
    try {
      return sequence(() -> inspectLocked(validateCompleteTrees));
    } catch (IOException e) {
      return new RepositoryState(
          Health.READ_ONLY, List.of(describe(e)), Optional.empty(), 0);
    }
  }

  private RepositoryState inspectLocked(boolean validateCompleteTrees) throws IOException {
    if (!exists()) {
      return new RepositoryState(Health.HEALTHY, List.of(), Optional.empty(), 0);
    }
    var diagnostics = new ArrayList<String>();
    Map<SnapshotId, GitRepositoryCore.CommitData> commits = new HashMap<>();
    Optional<SnapshotId> current = Optional.empty();
    try {
      if (!core.isBare()) {
        diagnostics.add("Internal repository is not bare");
      }
      for (var ref : core.refsByPrefix(SNAPSHOTS_PREFIX)) {
        String suffix = ref.name().substring(SNAPSHOTS_PREFIX.length());
        if (!suffix.equals(ref.target().value())) {
          diagnostics.add("Snapshot retention ref name does not match its target: " + ref.name());
        }
        try {
          var commit = core.readCommit(ref.target());
          if (validateCompleteTrees) {
            core.summarizeTree(commit.treeId(), limits);
          }
          commits.put(ref.target(), commit);
        } catch (IOException e) {
          diagnostics.add("Invalid snapshot commit " + ref.target() + ": " + e.getMessage());
        }
      }
      current = core.exactRef(CURRENT_REF);
      if (current.isPresent() && !commits.containsKey(current.orElseThrow())) {
        diagnostics.add("Current ref does not point to a retained snapshot");
      }
      if (!commits.isEmpty() && current.isEmpty()) {
        diagnostics.add("Repository has snapshots but no current baseline");
      }
      long roots = 0;
      for (var commit : commits.values()) {
        if (commit.parents().size() > 1) {
          diagnostics.add("Snapshot has multiple parents: " + commit.id());
        } else if (commit.parents().isEmpty()) {
          roots++;
        } else if (!commits.containsKey(commit.parents().getFirst())) {
          diagnostics.add("Snapshot parent is not retained: " + commit.id());
        }
      }
      if (!commits.isEmpty() && roots != 1) {
        diagnostics.add("Snapshot history must contain exactly one root; found " + roots);
      }
    } catch (Exception e) {
      diagnostics.add("Repository inspection failed: " + describe(e));
    }
    return new RepositoryState(
        diagnostics.isEmpty() ? Health.HEALTHY : Health.READ_ONLY,
        diagnostics,
        current,
        commits.size());
  }

  /** Materializes the current snapshot as an editable baseline and returns its identity. */
  public Optional<SnapshotId> prepareSnapshotWorkspace(
      Path workspace, ProgressReporter progress) throws IOException {
    return sequence(
        () -> {
          initialize();
          requireHealthy();
          Optional<SnapshotId> baseline = core.exactRef(CURRENT_REF);
          if (baseline.isPresent()) {
            core.exportCommitTree(baseline.orElseThrow(), workspace, limits, progress);
          } else {
            Files.createDirectories(workspace);
          }
          return baseline;
        });
  }

  public SnapshotId saveSnapshot(Path workspace, SaveMetadata metadata, ProgressReporter progress)
      throws IOException {
    return sequence(
        () -> {
          initialize();
          requireHealthy();
          return saveSnapshot(workspace, core.exactRef(CURRENT_REF), metadata, progress);
        });
  }

  /** Saves a workspace derived from the declared parent, rejecting stale baselines. */
  public SnapshotId saveSnapshot(
      Path workspace,
      Optional<SnapshotId> expectedParent,
      SaveMetadata metadata,
      ProgressReporter progress)
      throws IOException {
    return sequence(
        () -> {
          initialize();
          requireHealthy();
          Optional<SnapshotId> current = core.exactRef(CURRENT_REF);
          if (!current.equals(expectedParent)) {
            throw new ConcurrentUpdateException(
                "Current snapshot changed while the workspace was being edited");
          }
          validateWorkspaceShape(workspace);

          SnapshotId tree = core.writeTree(workspace, limits, progress);
          UUID operationId = UUID.randomUUID();
          String message = encodeMessage(metadata, operationId);
          SnapshotId commit =
              core.createCommit(
                  tree,
                  expectedParent.stream().toList(),
                  message,
                  metadata.author(),
                  metadata.committer(),
                  metadata.timestamp());

          RefUpdate.Result retained =
              core.compareAndSetRef(snapshotRef(commit), Optional.empty(), commit, false);
          if (!successful(retained)) {
            throw new IOException("Failed to retain new snapshot: " + retained);
          }
          RefUpdate.Result activated =
              core.compareAndSetRef(CURRENT_REF, expectedParent, commit, false);
          if (!successful(activated)) {
            throw new ConcurrentUpdateException(
                "Current snapshot changed while saving; new snapshot remains retained (" + activated + ")");
          }
          return commit;
        });
  }

  public SnapshotTreePage queryTree(
      UUID parcelUuid, int limit, Optional<SnapshotId> cursor) throws IOException {
    if (limit < 1) {
      throw new IllegalArgumentException("Snapshot tree page limit must be positive");
    }
    return readSequenced(
        () -> {
          if (!exists()) {
            return new SnapshotTreePage(
                parcelUuid, cursor, List.of(), Optional.empty(), Optional.empty(), Optional.empty());
          }
          requireHealthy();
          var commits = retainedCommits();
          commits.sort(
              Comparator.comparing(GitRepositoryCore.CommitData::committedAt)
                  .thenComparing(data -> data.id().value())
                  .reversed());
          int start = 0;
          if (cursor.isPresent()) {
            int index = -1;
            for (int i = 0; i < commits.size(); i++) {
              if (commits.get(i).id().equals(cursor.orElseThrow())) {
                index = i;
                break;
              }
            }
            if (index < 0) {
              throw new IOException("Invalid or stale snapshot cursor");
            }
            start = index + 1;
          }
          int end = Math.min(commits.size(), start + limit);
          var nodes = new ArrayList<SnapshotNode>(end - start);
          for (int i = start; i < end; i++) {
            nodes.add(toNode(commits.get(i)));
          }
          Optional<SnapshotId> next =
              end < commits.size() && !nodes.isEmpty()
                  ? Optional.of(nodes.getLast().id())
                  : Optional.empty();
          return new SnapshotTreePage(
              parcelUuid, cursor, nodes, core.exactRef(CURRENT_REF), next, Optional.empty());
        });
  }

  public Optional<SnapshotId> current() throws IOException {
    return sequence(
        () -> {
          if (!exists()) {
            return Optional.empty();
          }
          requireHealthy();
          return core.exactRef(CURRENT_REF);
        });
  }

  public void exportSnapshot(
      SnapshotId snapshot,
      Path destination,
      ProgressReporter progress)
      throws IOException {
    sequence(
        () -> {
          requireHealthy();
          requireRetained(snapshot);
          core.exportCommitTree(snapshot, destination, limits, progress);
          return null;
        });
  }

  /**
   * Validates, materializes, then applies a snapshot while keeping a durable operation ref across
   * the non-transactional world write.
   */
  public RestoreResult restoreSnapshot(
      SnapshotId target,
      SnapshotWorkspaceFactory workspaceFactory,
      SnapshotRestorer restorer,
      ProgressReporter progress)
      throws IOException {
    return sequence(() -> restoreLocked(target, workspaceFactory, restorer, progress));
  }

  private RestoreResult restoreLocked(
      SnapshotId target,
      SnapshotWorkspaceFactory workspaceFactory,
      SnapshotRestorer restorer,
      ProgressReporter progress)
      throws IOException {
    UUID operationId = UUID.randomUUID();
    Optional<SnapshotId> operationCommit = Optional.empty();
    requireHealthy();
    requireRetained(target);
    Optional<SnapshotId> before = core.exactRef(CURRENT_REF);
    RestoreOperation operation =
        new RestoreOperation(operationId, RestoreStage.PREPARING, target, before, "");
    operationCommit = Optional.of(writeOperation(operation, Optional.empty()));

    try (var workspace = workspaceFactory.create()) {
      Path snapshotRoot = workspace.root().resolve("snapshot");
      core.exportCommitTree(target, snapshotRoot, limits, progress);
      restorer.validate(snapshotRoot);

      operation = new RestoreOperation(operationId, RestoreStage.APPLYING, target, before, "");
      operationCommit = Optional.of(writeOperation(operation, operationCommit));
      try {
        restorer.apply(snapshotRoot);
      } catch (Exception e) {
        RestoreOperation failed =
            new RestoreOperation(operationId, RestoreStage.FAILED, target, before, describe(e));
        try {
          writeOperation(failed, operationCommit);
        } catch (IOException persistenceFailure) {
          e.addSuppressed(persistenceFailure);
        }
        throw new RestoreIncompleteException(operationId, "World restore did not complete", e);
      }
    } catch (RestoreIncompleteException e) {
      throw e;
    } catch (Exception e) {
      try {
        deleteOperation(operationId, operationCommit);
      } catch (IOException cleanupFailure) {
        e.addSuppressed(cleanupFailure);
      }
      if (e instanceof IOException io) {
        throw io;
      }
      throw new IOException("Snapshot validation failed", e);
    }

    RefUpdate.Result moved = core.compareAndSetRef(CURRENT_REF, before, target, true);
    if (!successful(moved)) {
      RestoreOperation failed =
          new RestoreOperation(
              operationId,
              RestoreStage.FAILED,
              target,
              before,
              "World was written but current ref update failed: " + moved);
      var incomplete = new RestoreIncompleteException(
          operationId, "World was restored but the current baseline could not be updated");
      try {
        writeOperation(failed, operationCommit);
      } catch (IOException persistenceFailure) {
        incomplete.addSuppressed(persistenceFailure);
      }
      throw incomplete;
    }
    deleteOperationQuietly(operationId, operationCommit);
    return new RestoreResult(operationId, target, before);
  }

  /**
   * Lists durable restore records. A single damaged operation ref becomes a diagnostic instead of
   * failing the whole listing, so one unreadable record cannot hide other recoverable operations.
   */
  public PendingRestoreReport pendingRestores() throws IOException {
    return readSequenced(
        () -> {
          if (!exists()) {
            return new PendingRestoreReport(List.of(), List.of());
          }
          var result = new ArrayList<RestoreOperation>();
          var diagnostics = new ArrayList<String>();
          for (var ref : core.refsByPrefix(OPERATIONS_PREFIX)) {
            try {
              byte[] bytes = core.readSmallFile(ref.target(), OPERATION_FILE, 64 * 1024);
              result.add(decodeOperation(bytes));
            } catch (Exception e) {
              LOGGER.error("Unreadable restore operation ref {} in {}", ref.name(), path, e);
              diagnostics.add(ref.name() + ": " + describe(e));
            }
          }
          result.sort(Comparator.comparing(RestoreOperation::operationId));
          return new PendingRestoreReport(List.copyOf(result), List.copyOf(diagnostics));
        });
  }

  /** Retries an interrupted restore target, or restores its protected pre-operation snapshot. */
  public RestoreResult resolvePendingRestore(
      UUID pendingOperationId,
      boolean rollback,
      SnapshotWorkspaceFactory workspaceFactory,
      SnapshotRestorer restorer,
      ProgressReporter progress)
      throws IOException {
    return sequence(
        () -> {
          SnapshotId operationCommit =
              core.exactRef(operationRef(pendingOperationId))
                  .orElseThrow(
                      () -> new IOException("Unknown pending restore operation: " + pendingOperationId));
          RestoreOperation pending =
              decodeOperation(core.readSmallFile(operationCommit, OPERATION_FILE, 64 * 1024));
          if (!pending.operationId().equals(pendingOperationId)) {
            throw new IOException("Pending restore operation identity does not match its ref");
          }
          SnapshotId selected =
              rollback
                  ? pending
                      .before()
                      .orElseThrow(
                          () -> new IOException("Pending restore has no protected rollback snapshot"))
                  : pending.target();
          RestoreResult result = restoreLocked(selected, workspaceFactory, restorer, progress);
          deleteOperationQuietly(pendingOperationId, Optional.of(operationCommit));
          return result;
        });
  }

  private SnapshotNode toNode(GitRepositoryCore.CommitData commit) throws IOException {
    Message message = decodeMessage(commit.message());
    return new SnapshotNode(
        commit.id(),
        commit.parents().stream().findFirst(),
        message.name(),
        message.description(),
        commit.author(),
        commit.committedAt().toString(),
        message.source(),
        core.summarizeTree(commit.treeId(), limits));
  }

  private List<GitRepositoryCore.CommitData> retainedCommits() throws IOException {
    var commits = new ArrayList<GitRepositoryCore.CommitData>();
    for (var ref : core.refsByPrefix(SNAPSHOTS_PREFIX)) {
      commits.add(core.readCommit(ref.target()));
    }
    return commits;
  }

  private void requireHealthy() throws IOException {
    RepositoryState state = inspect(false);
    if (state.health() != Health.HEALTHY) {
      throw new RepositoryCorruptException(String.join("; ", state.diagnostics()));
    }
  }

  private void requireRetained(SnapshotId id) throws IOException {
    Optional<SnapshotId> retained = core.exactRef(snapshotRef(id));
    if (retained.isEmpty() || !retained.orElseThrow().equals(id)) {
      throw new IOException("Snapshot does not belong to this parcel: " + id);
    }
    core.readCommit(id);
  }

  private static void validateWorkspaceShape(Path workspace) throws IOException {
    if (!Files.isRegularFile(workspace.resolve("parcel.json"))) {
      throw new IOException("Snapshot workspace is missing parcel.json");
    }
    if (!Files.isDirectory(workspace.resolve("data"))) {
      throw new IOException("Snapshot workspace is missing data directory");
    }
    try (var dataEntries = Files.walk(workspace.resolve("data"))) {
      if (dataEntries.noneMatch(Files::isRegularFile)) {
        throw new IOException("Snapshot data directory must contain at least one regular file");
      }
    }
    try (var entries = Files.newDirectoryStream(workspace)) {
      for (Path entry : entries) {
        String name = entry.getFileName().toString();
        if (!name.equals("parcel.json") && !name.equals("data")) {
          throw new IOException("Unexpected snapshot root entry: " + name);
        }
      }
    }
  }

  private SnapshotId writeOperation(
      RestoreOperation operation, Optional<SnapshotId> expectedCommit) throws IOException {
    byte[] json = GSON.toJson(encodeOperation(operation)).getBytes(StandardCharsets.UTF_8);
    SnapshotId tree = core.writeSmallTree(Map.of(OPERATION_FILE, json), limits);
    Instant now = Instant.now();
    var server = new GitRepositoryCore.Identity("Git Parcel Server", "server@gitparcel.local");
    SnapshotId commit =
        core.createCommit(tree, List.of(), "Git Parcel restore operation", server, server, now);
    RefUpdate.Result result =
        core.compareAndSetRef(operationRef(operation.operationId()), expectedCommit, commit, true);
    if (!successful(result)) {
      throw new IOException("Failed to persist restore operation: " + result);
    }
    return commit;
  }

  private void deleteOperation(UUID operationId, Optional<SnapshotId> expected) throws IOException {
    RefUpdate.Result result = core.deleteRef(operationRef(operationId), expected);
    if (result != RefUpdate.Result.NO_CHANGE && result != RefUpdate.Result.FORCED) {
      throw new IOException("Failed to clear completed restore operation: " + result);
    }
  }

  /**
   * Clears the durable record after a fully successful restore. The world and the current baseline
   * are already correct at this point, so a cleanup failure must not surface as a restore failure;
   * it only leaves a stale operation ref behind and logs a warning.
   */
  private void deleteOperationQuietly(UUID operationId, Optional<SnapshotId> expected) {
    try {
      deleteOperation(operationId, expected);
    } catch (IOException e) {
      LOGGER.warn(
          "Failed to clear completed restore operation {} in {}; a stale operation ref remains"
              + " and will be reported by the next audit",
          operationId,
          path,
          e);
    }
  }

  private static JsonObject encodeOperation(RestoreOperation operation) {
    var json = new JsonObject();
    json.addProperty("schema", 1);
    json.addProperty("operation_id", operation.operationId().toString());
    json.addProperty("stage", operation.stage().name().toLowerCase());
    json.addProperty("target", operation.target().value());
    operation.before().ifPresent(value -> json.addProperty("before", value.value()));
    if (!operation.error().isBlank()) {
      json.addProperty("error", operation.error());
    }
    return json;
  }

  private static RestoreOperation decodeOperation(byte[] bytes) throws IOException {
    try {
      JsonObject json = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
      if (json.get("schema").getAsInt() != 1) {
        throw new IllegalArgumentException("Unsupported operation schema");
      }
      return new RestoreOperation(
          UUID.fromString(json.get("operation_id").getAsString()),
          RestoreStage.valueOf(json.get("stage").getAsString().toUpperCase(Locale.ROOT)),
          new SnapshotId(json.get("target").getAsString()),
          json.has("before")
              ? Optional.of(new SnapshotId(json.get("before").getAsString()))
              : Optional.empty(),
          json.has("error") ? json.get("error").getAsString() : "");
    } catch (RuntimeException e) {
      throw new IOException("Invalid restore operation metadata", e);
    }
  }

  private static String encodeMessage(SaveMetadata metadata, UUID operationId) {
    String name = oneLine(metadata.name());
    String description = metadata.description().strip();
    return name
        + (description.isEmpty() ? "" : "\n\n" + description)
        + "\n\n"
        + TRAILER_PREFIX
        + "Source: "
        + metadata.source().name().toLowerCase(Locale.ROOT)
        + "\n"
        + TRAILER_PREFIX
        + "Operation: "
        + operationId
        + "\n";
  }

  /**
   * Splits a commit message into its player-facing text and machine trailers. Trailer lines are
   * {@code Git-Parcel-Key: value} entries in the final paragraph; a trailer-like line that does
   * not parse is corruption instead of being silently ignored.
   */
  private static Message decodeMessage(String value) throws IOException {
    String[] lines = value.split("\\R", -1);
    int trailerStart = lines.length;
    while (trailerStart > 0 && lines[trailerStart - 1].isBlank()) {
      trailerStart--;
    }
    while (trailerStart > 0 && lines[trailerStart - 1].startsWith(TRAILER_PREFIX)) {
      trailerStart--;
    }
    SnapshotNode.Source source = SnapshotNode.Source.SAVED;
    for (int i = trailerStart; i < lines.length; i++) {
      String line = lines[i];
      if (line.isBlank()) {
        continue;
      }
      if (!line.startsWith(TRAILER_PREFIX)) {
        throw new RepositoryCorruptException("Snapshot commit message has a malformed trailer: " + line);
      }
      int separator = line.indexOf(':', TRAILER_PREFIX.length());
      if (separator < 0) {
        throw new RepositoryCorruptException("Snapshot commit message has a malformed trailer: " + line);
      }
      String key = line.substring(TRAILER_PREFIX.length(), separator);
      String trailerValue = line.substring(separator + 1).strip();
      if (trailerValue.isBlank()) {
        throw new RepositoryCorruptException("Snapshot commit message has a blank trailer: " + line);
      }
      switch (key) {
        case "Source" -> {
          try {
            source = SnapshotNode.Source.valueOf(trailerValue.toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException ignored) {
            // Unknown future source values remain readable as a regular saved snapshot.
          }
        }
        default -> {
          // Unknown future trailer keys are ignored so newer snapshots stay queryable.
        }
      }
    }
    String playerText = String.join("\n", java.util.Arrays.asList(lines).subList(0, trailerStart)).strip();
    String[] text = playerText.split("\\R\\R", 2);
    String name = text.length == 0 || text[0].isBlank() ? "Snapshot" : oneLine(text[0]);
    String description = text.length > 1 ? text[1].strip() : "";
    return new Message(name, description, source);
  }

  private static String oneLine(String value) {
    String result = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').strip();
    return result.isEmpty() ? "Snapshot" : result;
  }

  private static String snapshotRef(SnapshotId id) {
    return SNAPSHOTS_PREFIX + id.value();
  }

  private static String operationRef(UUID id) {
    return OPERATIONS_PREFIX + id;
  }

  private static boolean successful(RefUpdate.Result result) {
    return result == RefUpdate.Result.NEW
        || result == RefUpdate.Result.FAST_FORWARD
        || result == RefUpdate.Result.FORCED
        || result == RefUpdate.Result.NO_CHANGE;
  }

  /** Runs a multi-step repository sequence under the shared per-path lock (reentrant). */
  private <T> T sequence(GitRepositoryCore.LockedAction<T> action) throws IOException {
    try {
      return core.withLock(action);
    } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
      throw new IOException("Unexpected JGit failure for " + path, e);
    }
  }

  /** Runs a read-style sequence with a bounded lock wait that fails fast instead of blocking. */
  private <T> T readSequenced(GitRepositoryCore.LockedAction<T> action) throws IOException {
    try {
      return core.tryWithLock(READ_LOCK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS, action);
    } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
      throw new IOException("Unexpected JGit failure for " + path, e);
    }
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null || message.isBlank() ? "" : ": " + message);
  }

  public enum Health {
    HEALTHY,
    READ_ONLY
  }

  public enum RestoreStage {
    PREPARING,
    APPLYING,
    FAILED
  }

  public record RepositoryState(
      Health health,
      List<String> diagnostics,
      Optional<SnapshotId> current,
      int snapshotCount) {
    public RepositoryState {
      diagnostics = List.copyOf(diagnostics);
      current = current == null ? Optional.empty() : current;
    }
  }

  public record SaveMetadata(
      String name,
      String description,
      GitRepositoryCore.Identity author,
      GitRepositoryCore.Identity committer,
      SnapshotNode.Source source,
      Instant timestamp) {
    public SaveMetadata {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Snapshot name must not be blank");
      }
      description = description == null ? "" : description;
      timestamp = timestamp == null ? Instant.now() : timestamp;
    }
  }

  public record RestoreOperation(
      UUID operationId,
      RestoreStage stage,
      SnapshotId target,
      Optional<SnapshotId> before,
      String error) {
    public RestoreOperation {
      before = before == null ? Optional.empty() : before;
      error = error == null ? "" : error;
    }
  }

  /** Durable restore records plus diagnostics for operation refs that could not be decoded. */
  public record PendingRestoreReport(
      List<RestoreOperation> operations, List<String> diagnostics) {
    public PendingRestoreReport {
      operations = List.copyOf(operations);
      diagnostics = List.copyOf(diagnostics);
    }

    public boolean isEmpty() {
      return operations.isEmpty() && diagnostics.isEmpty();
    }
  }

  public record RestoreResult(
      UUID operationId, SnapshotId restored, Optional<SnapshotId> previous) {
    public RestoreResult {
      previous = previous == null ? Optional.empty() : previous;
    }
  }

  public interface SnapshotRestorer {
    void validate(Path snapshotRoot) throws Exception;

    void apply(Path snapshotRoot) throws Exception;
  }

  private record Message(String name, String description, SnapshotNode.Source source) {}

  public static class RepositoryCorruptException extends IOException {
    public RepositoryCorruptException(String message) {
      super(message);
    }
  }

  public static class ConcurrentUpdateException extends IOException {
    public ConcurrentUpdateException(String message) {
      super(message);
    }
  }

  public static class RestoreIncompleteException extends IOException {
    private final UUID operationId;

    public RestoreIncompleteException(UUID operationId, String message) {
      super(message);
      this.operationId = operationId;
    }

    public RestoreIncompleteException(UUID operationId, String message, Throwable cause) {
      super(message, cause);
      this.operationId = operationId;
    }

    public UUID operationId() {
      return operationId;
    }
  }
}
