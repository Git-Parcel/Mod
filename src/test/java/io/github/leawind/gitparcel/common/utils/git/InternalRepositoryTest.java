package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.impl.snapshot.TemporarySnapshotWorkspaceFactory;
import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.RefUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InternalRepositoryTest {
  private static final GitRepositoryCore.Identity PLAYER =
      new GitRepositoryCore.Identity("Player", "player@gitparcel.local");
  private static final GitRepositoryCore.Identity SERVER =
      new GitRepositoryCore.Identity("Server", "server@gitparcel.local");

  @TempDir Path tempDir;

  @Test
  void createsBareSingleParentHistoryAndAlwaysCommits() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    Path workspace = workspace("stone");

    var first = repository.saveSnapshot(workspace, metadata("First"), ProgressReporter.NONE);
    var second = repository.saveSnapshot(workspace, metadata("Second"), ProgressReporter.NONE);

    assertNotEquals(first, second);
    assertTrue(repository.path().getFileName().toString().endsWith(".git"));
    assertFalse(Files.exists(repository.path().resolve(".git")));
    try (var git = Git.open(repository.path().toFile())) {
      assertTrue(git.getRepository().isBare());
      assertEquals(InternalRepository.CURRENT_REF, git.getRepository().getFullBranch());
    }

    var page = repository.queryTree(UUID.randomUUID(), 10, java.util.Optional.empty());
    assertEquals(2, page.nodes().size());
    assertEquals(second, page.current().orElseThrow());
    var secondNode = page.nodes().stream().filter(node -> node.id().equals(second)).findFirst().orElseThrow();
    assertEquals(first, secondNode.parentId().orElseThrow());
    assertEquals(2, repository.inspect().snapshotCount());
  }

  @Test
  void materializesAndChecksAnExplicitSnapshotBaseline() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first =
        repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    Path edit = tempDir.resolve("edit");

    Optional<io.github.leawind.gitparcel.common.api.snapshot.SnapshotId> baseline =
        repository.prepareSnapshotWorkspace(edit, ProgressReporter.NONE);

    assertEquals(first, baseline.orElseThrow());
    assertEquals("one", Files.readString(edit.resolve("data/content.txt")));
    Files.writeString(edit.resolve("data/content.txt"), "two");
    var second =
        repository.saveSnapshot(edit, baseline, metadata("Two"), ProgressReporter.NONE);
    assertEquals(second, repository.current().orElseThrow());

    Path staleEdit = tempDir.resolve("stale-edit");
    Optional<io.github.leawind.gitparcel.common.api.snapshot.SnapshotId> staleBaseline =
        repository.prepareSnapshotWorkspace(staleEdit, ProgressReporter.NONE);
    repository.saveSnapshot(workspace("three"), metadata("Three"), ProgressReporter.NONE);
    assertThrows(
        InternalRepository.ConcurrentUpdateException.class,
        () ->
            repository.saveSnapshot(
                staleEdit, staleBaseline, metadata("Stale"), ProgressReporter.NONE));
  }

  @Test
  void restoringOlderSnapshotThenSavingCreatesAVisibleFork() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    var originalChild =
        repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);

    repository.restoreSnapshot(
        first,
        TemporarySnapshotWorkspaceFactory.INSTANCE,
        new InternalRepository.SnapshotRestorer() {
          @Override
          public void validate(Path snapshotRoot) {}

          @Override
          public void apply(Path snapshotRoot) throws Exception {
            assertEquals("one", Files.readString(snapshotRoot.resolve("data/content.txt")));
          }
        },
        ProgressReporter.NONE);
    var fork = repository.saveSnapshot(workspace("three"), metadata("Fork"), ProgressReporter.NONE);

    var page = repository.queryTree(UUID.randomUUID(), 10, java.util.Optional.empty());
    var byId = page.nodes().stream().collect(java.util.stream.Collectors.toMap(SnapshotNode::id, node -> node));
    assertEquals(first, byId.get(originalChild).parentId().orElseThrow());
    assertEquals(first, byId.get(fork).parentId().orElseThrow());
    assertEquals(fork, page.current().orElseThrow());
    assertEquals(3, page.nodes().size());
  }

  @Test
  void failedWorldWriteLeavesDurableRecoveryOperationAndDoesNotMoveCurrent() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    var second = repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);

    var failure =
        assertThrows(
            InternalRepository.RestoreIncompleteException.class,
            () ->
                repository.restoreSnapshot(
                    first,
                    TemporarySnapshotWorkspaceFactory.INSTANCE,
                    new InternalRepository.SnapshotRestorer() {
                      @Override
                      public void validate(Path snapshotRoot) {}

                      @Override
                      public void apply(Path snapshotRoot) throws Exception {
                        throw new java.io.IOException("simulated world failure");
                      }
                    },
                    ProgressReporter.NONE));

    assertEquals(second, repository.current().orElseThrow());
    var pending = new InternalRepository(repository.path()).pendingRestores();
    assertEquals(1, pending.size());
    assertEquals(failure.operationId(), pending.getFirst().operationId());
    assertEquals(InternalRepository.RestoreStage.FAILED, pending.getFirst().stage());
    assertEquals(first, pending.getFirst().target());
    assertEquals(second, pending.getFirst().before().orElseThrow());
  }

  @Test
  void pendingRestoreCanBeRetriedAfterRepositoryReopen() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);
    var failure =
        assertThrows(
            InternalRepository.RestoreIncompleteException.class,
            () ->
                repository.restoreSnapshot(
                    first,
                    TemporarySnapshotWorkspaceFactory.INSTANCE,
                    new InternalRepository.SnapshotRestorer() {
                      @Override
                      public void validate(Path snapshotRoot) {}

                      @Override
                      public void apply(Path snapshotRoot) throws Exception {
                        throw new java.io.IOException("interrupted");
                      }
                    },
                    ProgressReporter.NONE));

    var reopened = new InternalRepository(repository.path());
    reopened.resolvePendingRestore(
        failure.operationId(),
        false,
        TemporarySnapshotWorkspaceFactory.INSTANCE,
        new InternalRepository.SnapshotRestorer() {
          @Override
          public void validate(Path snapshotRoot) {}

          @Override
          public void apply(Path snapshotRoot) throws Exception {
            assertEquals("one", Files.readString(snapshotRoot.resolve("data/content.txt")));
          }
        },
        ProgressReporter.NONE);

    assertEquals(first, reopened.current().orElseThrow());
    assertTrue(reopened.pendingRestores().isEmpty());
  }

  @Test
  void pendingRestoreCanRollBackToProtectedSnapshot() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    var second = repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);
    var failure =
        assertThrows(
            InternalRepository.RestoreIncompleteException.class,
            () ->
                repository.restoreSnapshot(
                    first,
                    TemporarySnapshotWorkspaceFactory.INSTANCE,
                    failingRestorer(),
                    ProgressReporter.NONE));

    repository.resolvePendingRestore(
        failure.operationId(),
        true,
        TemporarySnapshotWorkspaceFactory.INSTANCE,
        new InternalRepository.SnapshotRestorer() {
          @Override
          public void validate(Path snapshotRoot) {}

          @Override
          public void apply(Path snapshotRoot) throws Exception {
            assertEquals("two", Files.readString(snapshotRoot.resolve("data/content.txt")));
          }
        },
        ProgressReporter.NONE);

    assertEquals(second, repository.current().orElseThrow());
    assertTrue(repository.pendingRestores().isEmpty());
  }

  @Test
  void rejectsNonSnapshotIdsAndUnsafeWorkspaceEntries() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    Path workspace = workspace("safe");
    Files.writeString(workspace.resolve("unexpected.txt"), "nope");

    assertThrows(
        java.io.IOException.class,
        () -> repository.saveSnapshot(workspace, metadata("Unsafe"), ProgressReporter.NONE));
  }

  @Test
  void internalPolicyCannotRequestSharedCapabilities() {
    assertThrows(
        SecurityException.class,
        () -> RepositoryPolicy.INTERNAL.require(RepositoryCapability.REMOTES));
    assertTrue(RepositoryPolicy.SHARED.permits(RepositoryCapability.REMOTES));
  }

  @Test
  void bridgesArbitraryNioWorkspacesWithoutMovingTheRepository() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    try (var memory = Jimfs.newFileSystem()) {
      Path workspace = memory.getPath("/snapshot");
      Files.createDirectories(workspace.resolve("data"));
      Files.writeString(workspace.resolve("parcel.json"), "{}");
      Files.writeString(workspace.resolve("data/content.txt"), "portable");
      var snapshot =
          repository.saveSnapshot(workspace, metadata("Portable"), ProgressReporter.NONE);

      Path zip = tempDir.resolve("export.zip");
      try (var archive = FileSystems.newFileSystem(zip, Map.of("create", "true"))) {
        Path exported = archive.getPath("/snapshot");
        repository.exportSnapshot(snapshot, exported, ProgressReporter.NONE);
        assertEquals("portable", Files.readString(exported.resolve("data/content.txt")));
      }
    }
    assertTrue(repository.path().getFileSystem().equals(tempDir.getFileSystem()));
  }

  @Test
  void paginatesByOpaqueSnapshotCursor() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);
    repository.saveSnapshot(workspace("three"), metadata("Three"), ProgressReporter.NONE);

    var first = repository.queryTree(UUID.randomUUID(), 1, Optional.empty());
    var second =
        repository.queryTree(UUID.randomUUID(), 1, first.nextCursor());

    assertEquals(1, first.nodes().size());
    assertEquals(1, second.nodes().size());
    assertNotEquals(first.nodes().getFirst().id(), second.nodes().getFirst().id());
    assertThrows(
        java.io.IOException.class,
        () ->
            repository.queryTree(
                UUID.randomUUID(),
                1,
                Optional.of(new io.github.leawind.gitparcel.common.api.snapshot.SnapshotId(
                    "0".repeat(40)))));
  }

  @Test
  void retainedForksSurviveGitGarbageCollection() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var root = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);
    repository.restoreSnapshot(
        root,
        TemporarySnapshotWorkspaceFactory.INSTANCE,
        new InternalRepository.SnapshotRestorer() {
          @Override
          public void validate(Path snapshotRoot) {}

          @Override
          public void apply(Path snapshotRoot) {}
        },
        ProgressReporter.NONE);
    repository.saveSnapshot(workspace("fork"), metadata("Fork"), ProgressReporter.NONE);

    try (var git = Git.open(repository.path().toFile())) {
      git.gc().call();
    }

    assertEquals(
        3,
        repository.queryTree(UUID.randomUUID(), 10, Optional.empty()).nodes().size());
    assertEquals(InternalRepository.Health.HEALTHY, repository.inspect().health());
  }

  @Test
  void compareAndSetDoesNotOverwriteAChangedCurrentRef() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var first = repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE);
    var core = new GitRepositoryCore(repository.path(), RepositoryPolicy.INTERNAL);

    RefUpdate.Result result =
        core.compareAndSetRef(
            InternalRepository.CURRENT_REF, Optional.of(first), first, true);

    assertFalse(
        Set.of(
                RefUpdate.Result.NEW,
                RefUpdate.Result.FAST_FORWARD,
                RefUpdate.Result.FORCED,
                RefUpdate.Result.NO_CHANGE)
            .contains(result));
  }

  @Test
  void detectsMissingSnapshotObjectsInsteadOfReinitializing() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    var snapshot =
        repository.saveSnapshot(workspace("one"), metadata("One"), ProgressReporter.NONE);
    Path looseObject =
        repository
            .path()
            .resolve("objects")
            .resolve(snapshot.value().substring(0, 2))
            .resolve(snapshot.value().substring(2));
    Files.delete(looseObject);

    var state = repository.inspect();
    assertEquals(InternalRepository.Health.READ_ONLY, state.health());
    assertFalse(state.diagnostics().isEmpty());
    assertThrows(
        InternalRepository.RepositoryCorruptException.class,
        () -> repository.saveSnapshot(workspace("two"), metadata("Two"), ProgressReporter.NONE));
  }

  @Test
  void rejectsCaseAmbiguousWorkspaceTrees() throws Exception {
    var repository = InternalRepository.at(tempDir, UUID.randomUUID());
    Path workspace = workspace("safe");
    Files.createDirectories(workspace.resolve("data/A"));
    Files.createDirectories(workspace.resolve("data/a"));
    Files.writeString(workspace.resolve("data/A/value.txt"), "one");
    Files.writeString(workspace.resolve("data/a/value.txt"), "two");

    assertThrows(
        java.io.IOException.class,
        () -> repository.saveSnapshot(workspace, metadata("Ambiguous"), ProgressReporter.NONE));
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

  private static InternalRepository.SnapshotRestorer failingRestorer() {
    return new InternalRepository.SnapshotRestorer() {
      @Override
      public void validate(Path snapshotRoot) {}

      @Override
      public void apply(Path snapshotRoot) throws Exception {
        throw new java.io.IOException("interrupted");
      }
    };
  }
}
