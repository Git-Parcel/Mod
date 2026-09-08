package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedRepositoryTest {
  private static final GitRepositoryCore.Identity IDENTITY =
      new GitRepositoryCore.Identity("Test Player", "gitparcel@localhost");

  @TempDir Path tempDir;

  @Test
  void coreOperationsEnforceTheirPolicyIndependentlyOfCommandVisibility() throws Exception {
    var denied = new RepositoryPolicy(java.util.Set.of());
    var core = new GitRepositoryCore(tempDir.resolve("repo"), denied);
    assertThrows(SecurityException.class, () -> core.exactRef("refs/heads/main"));
    assertThrows(
        SecurityException.class,
        () -> core.readSmallFile(new io.github.leawind.gitparcel.common.api.snapshot.SnapshotId(
            "0123456789abcdef0123456789abcdef01234567"), "parcel.json", 1024));
  }

  @Test
  void commitsHistoryAndExportsOnlyTheParcelPath() throws Exception {
    Path repositoryDir = tempDir.resolve("repository");
    Path parcel = repositoryDir.resolve("parcel");
    Path unrelated = repositoryDir.resolve("unrelated");
    Files.createDirectories(parcel);
    Files.createDirectories(unrelated);
    Files.writeString(parcel.resolve("parcel.json"), "first");
    Files.writeString(unrelated.resolve("keep.txt"), "not part of the commit");

    SharedRepository repository = SharedRepository.get(repositoryDir);
    var first = repository.commit("parcel", "First snapshot", IDENTITY).orElseThrow();

    Files.writeString(parcel.resolve("parcel.json"), "second");
    Files.writeString(parcel.resolve("new.txt"), "new file");
    var second = repository.commit("parcel", "Second snapshot", IDENTITY).orElseThrow();

    assertTrue(repository.commit("parcel", "No changes", IDENTITY).isEmpty());

    Files.delete(parcel.resolve("new.txt"));
    var third = repository.commit("parcel", "Remove file", IDENTITY).orElseThrow();

    var history = repository.historyPage("parcel", 10, null).commits();
    assertEquals(3, history.size());
    assertEquals(third.revision(), history.get(0).revision());
    assertEquals("Remove file", history.get(0).message());
    assertEquals(second.revision(), history.get(1).revision());
    assertEquals("Second snapshot", history.get(1).message());
    assertEquals(first.revision(), history.get(2).revision());
    assertEquals("Test Player", history.get(2).author());
    assertTrue(repository.historyPage("unrelated", 10, null).commits().isEmpty());

    Path firstExport = tempDir.resolve("first-export");
    repository.exportRevision(first.revision(), "parcel", firstExport);
    assertEquals("first", Files.readString(firstExport.resolve("parcel.json")));
    assertFalse(Files.exists(firstExport.resolve("new.txt")));
    assertFalse(Files.exists(firstExport.resolve("unrelated")));

    Path secondExport = tempDir.resolve("second-export");
    repository.exportRevision(second.revision(), "parcel", secondExport);
    assertEquals("second", Files.readString(secondExport.resolve("parcel.json")));
    assertEquals("new file", Files.readString(secondExport.resolve("new.txt")));

    Path thirdExport = tempDir.resolve("third-export");
    repository.exportRevision(third.revision(), "parcel", thirdExport);
    assertEquals("second", Files.readString(thirdExport.resolve("parcel.json")));
    assertFalse(Files.exists(thirdExport.resolve("new.txt")));
  }

  @Test
  void missingRepositoryHasNoHistoryAndCannotBeExported() throws Exception {
    SharedRepository repository = SharedRepository.get(tempDir.resolve("missing"));

    assertTrue(repository.historyPage("parcel", 10, null).commits().isEmpty());
    assertThrows(
        IOException.class,
        () ->
            repository.exportRevision(
                "HEAD", "parcel", tempDir.resolve("missing-export")));
  }

  @Test
  void rejectsPathsOutsideTheRepository() {
    SharedRepository repository = SharedRepository.get(tempDir);

    assertThrows(
        IllegalArgumentException.class,
        () -> repository.commit("../parcel", "Invalid", IDENTITY));
    assertThrows(
        IllegalArgumentException.class,
        () -> repository.historyPage("/parcel", 10, null));
  }

  @Test
  void clonesFetchesPullsAndPushesAgainstLocalRemote() throws Exception {
    Path origin = tempDir.resolve("origin.git");
    try (Git ignored =
        Git.init()
            .setBare(true)
            .setInitialBranch("main")
            .setDirectory(origin.toFile())
            .call()) {}

    Path seed = tempDir.resolve("seed");
    try (Git ignored =
        Git.init()
            .setInitialBranch("main")
            .setDirectory(seed.toFile())
            .call()) {}
    Files.createDirectories(seed.resolve("parcel"));
    Files.writeString(seed.resolve("parcel/parcel.json"), "initial");
    SharedRepository.get(seed).commit("parcel", "Initial", IDENTITY).orElseThrow();
    try (Git git = Git.open(seed.toFile())) {
      var config = git.getRepository().getConfig();
      config.setString(
          ConfigConstants.CONFIG_REMOTE_SECTION,
          "origin",
          ConfigConstants.CONFIG_KEY_URL,
          origin.toUri().toString());
      config.save();
      git.push()
          .setRemote("origin")
          .setRefSpecs(new RefSpec("HEAD:refs/heads/main"))
          .call();
    }

    SharedRepository first =
        SharedRepository.cloneRepository(
            origin.toUri().toString(), tempDir.resolve("first"), null);
    SharedRepository second =
        SharedRepository.cloneRepository(
            origin.toUri().toString(), tempDir.resolve("second"), null);

    Files.writeString(first.path().resolve("parcel/parcel.json"), "updated");
    first.commit("parcel", "Update", IDENTITY).orElseThrow();
    assertEquals(1, first.push(null));

    assertTrue(second.fetch(null) >= 1);
    assertEquals("initial", Files.readString(second.path().resolve("parcel/parcel.json")));
    second.pull(null);
    assertEquals("updated", Files.readString(second.path().resolve("parcel/parcel.json")));

    Files.writeString(second.path().resolve("parcel/dirty.txt"), "dirty");
    assertThrows(IOException.class, () -> second.pull(null));
  }

  @Test
  void oneCommitCanAtomicallyIncludeParcelAndRepositoryMetadata() throws Exception {
    Path repositoryDir = tempDir.resolve("shared");
    Files.createDirectories(repositoryDir.resolve("parcels/example"));
    Files.writeString(
        repositoryDir.resolve("parcels/example/parcel.json"), "parcel");
    Files.writeString(repositoryDir.resolve("meta.json"), "metadata");
    SharedRepository repository = SharedRepository.get(repositoryDir);

    var commit =
        repository
            .commit(
                java.util.List.of("parcels/example", "meta.json"),
                "Publish parcel",
                IDENTITY)
            .orElseThrow();

    assertEquals(
        commit.revision(),
        repository.historyPage("parcels/example", 1, null).commits().getFirst().revision());
    assertEquals(
        commit.revision(),
        repository.historyPage("meta.json", 1, null).commits().getFirst().revision());
  }

  @Test
  void resetsPublicationPathsInUnbornAndExistingRepositories() throws Exception {
    Path unbornDir = tempDir.resolve("unborn");
    SharedRepository unborn = SharedRepository.get(unbornDir);
    unborn.initialize();
    Files.createDirectories(unbornDir.resolve("parcel"));
    Files.writeString(unbornDir.resolve("parcel/parcel.json"), "new");
    Files.writeString(unbornDir.resolve("meta.json"), "new");
    try (Git git = Git.open(unbornDir.toFile())) {
      git.add().addFilepattern("parcel").call();
      git.add().addFilepattern("meta.json").call();
    }
    Files.delete(unbornDir.resolve("parcel/parcel.json"));
    Files.delete(unbornDir.resolve("meta.json"));
    unborn.resetIndexPaths(List.of("parcel", "meta.json"));
    try (Git git = Git.open(unbornDir.toFile())) {
      assertFalse(git.status().call().hasUncommittedChanges());
    }

    Path existingDir = tempDir.resolve("existing");
    Files.createDirectories(existingDir.resolve("parcel"));
    Files.writeString(existingDir.resolve("parcel/parcel.json"), "old");
    Files.writeString(existingDir.resolve("meta.json"), "old");
    SharedRepository existing = SharedRepository.get(existingDir);
    existing.commit(List.of("parcel", "meta.json"), "Initial", IDENTITY).orElseThrow();
    Files.writeString(existingDir.resolve("parcel/parcel.json"), "new");
    Files.writeString(existingDir.resolve("meta.json"), "new");
    try (Git git = Git.open(existingDir.toFile())) {
      git.add().addFilepattern("parcel").call();
      git.add().addFilepattern("meta.json").call();
    }
    Files.writeString(existingDir.resolve("parcel/parcel.json"), "old");
    Files.writeString(existingDir.resolve("meta.json"), "old");
    existing.resetIndexPaths(List.of("parcel", "meta.json"));
    try (Git git = Git.open(existingDir.toFile())) {
      assertFalse(git.status().call().hasUncommittedChanges());
    }
  }

  @Test
  void paginatesHistoryWithStableRevisionCursors() throws Exception {
    Path repositoryDir = tempDir.resolve("history-pages");
    Path parcelFile = repositoryDir.resolve("parcel/parcel.json");
    Files.createDirectories(parcelFile.getParent());
    SharedRepository repository = SharedRepository.get(repositoryDir);

    for (int i = 1; i <= 5; i++) {
      Files.writeString(parcelFile, "version " + i);
      repository.commit("parcel", "Version " + i, IDENTITY).orElseThrow();
    }

    var first = repository.historyPage("parcel", 2, null);
    assertEquals(List.of("Version 5", "Version 4"), messages(first));
    assertTrue(first.nextCursor().isPresent());

    Files.writeString(parcelFile, "version 6");
    repository.commit("parcel", "Version 6", IDENTITY).orElseThrow();

    var second =
        repository.historyPage("parcel", 2, first.nextCursor().orElseThrow());
    assertEquals(List.of("Version 3", "Version 2"), messages(second));
    assertTrue(second.nextCursor().isPresent());

    var third =
        repository.historyPage("parcel", 2, second.nextCursor().orElseThrow());
    assertEquals(List.of("Version 1"), messages(third));
    assertTrue(third.nextCursor().isEmpty());

    assertThrows(
        IOException.class,
        () -> repository.historyPage("parcel", 2, "0000000000000000000000000000000000000000"));

    Files.createDirectories(repositoryDir.resolve("unrelated"));
    Files.writeString(repositoryDir.resolve("unrelated/file.txt"), "unrelated");
    String unrelatedRevision =
        repository.commit("unrelated", "Unrelated", IDENTITY).orElseThrow().revision();
    assertThrows(
        IOException.class,
        () -> repository.historyPage("parcel", 2, unrelatedRevision));
  }

  private static List<String> messages(SharedRepository.HistoryPage page) {
    return page.commits().stream().map(SharedRepository.CommitInfo::message).toList();
  }
}
