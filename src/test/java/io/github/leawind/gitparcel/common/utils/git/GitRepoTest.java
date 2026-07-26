package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitRepoTest {
  private static final GitRepo.CommitIdentity IDENTITY =
      new GitRepo.CommitIdentity("Test Player", "gitparcel@localhost");

  @TempDir Path tempDir;

  @Test
  void commitsHistoryAndExportsOnlyTheParcelPath() throws Exception {
    Path repositoryDir = tempDir.resolve("repository");
    Path parcel = repositoryDir.resolve("parcel");
    Path unrelated = repositoryDir.resolve("unrelated");
    Files.createDirectories(parcel);
    Files.createDirectories(unrelated);
    Files.writeString(parcel.resolve("parcel.json"), "first");
    Files.writeString(unrelated.resolve("keep.txt"), "not part of the commit");

    GitRepo repository = GitRepo.get(repositoryDir);
    var first = repository.commit("parcel", "First snapshot", IDENTITY).orElseThrow();

    Files.writeString(parcel.resolve("parcel.json"), "second");
    Files.writeString(parcel.resolve("new.txt"), "new file");
    var second = repository.commit("parcel", "Second snapshot", IDENTITY).orElseThrow();

    assertTrue(repository.commit("parcel", "No changes", IDENTITY).isEmpty());

    Files.delete(parcel.resolve("new.txt"));
    var third = repository.commit("parcel", "Remove file", IDENTITY).orElseThrow();

    var history = repository.history("parcel", 10);
    assertEquals(3, history.size());
    assertEquals(third.revision(), history.get(0).revision());
    assertEquals("Remove file", history.get(0).message());
    assertEquals(second.revision(), history.get(1).revision());
    assertEquals("Second snapshot", history.get(1).message());
    assertEquals(first.revision(), history.get(2).revision());
    assertEquals("Test Player", history.get(2).author());
    assertTrue(repository.history("unrelated", 10).isEmpty());

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
    GitRepo repository = GitRepo.get(tempDir.resolve("missing"));

    assertTrue(repository.history("parcel", 10).isEmpty());
    assertThrows(
        IOException.class,
        () ->
            repository.exportRevision(
                "HEAD", "parcel", tempDir.resolve("missing-export")));
  }

  @Test
  void rejectsPathsOutsideTheRepository() {
    GitRepo repository = GitRepo.get(tempDir);

    assertThrows(
        IllegalArgumentException.class,
        () -> repository.commit("../parcel", "Invalid", IDENTITY));
    assertThrows(
        IllegalArgumentException.class,
        () -> repository.history("/parcel", 10));
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
    GitRepo.get(seed).commit("parcel", "Initial", IDENTITY).orElseThrow();
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

    GitRepo first =
        GitRepo.cloneRepository(
            origin.toUri().toString(), tempDir.resolve("first"), null);
    GitRepo second =
        GitRepo.cloneRepository(
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
    GitRepo repository = GitRepo.get(repositoryDir);

    var commit =
        repository
            .commit(
                java.util.List.of("parcels/example", "meta.json"),
                "Publish parcel",
                IDENTITY)
            .orElseThrow();

    assertEquals(
        commit.revision(),
        repository.history("parcels/example", 1).getFirst().revision());
    assertEquals(
        commit.revision(),
        repository.history("meta.json", 1).getFirst().revision());
  }
}
