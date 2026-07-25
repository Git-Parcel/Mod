package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
