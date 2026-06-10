package io.github.leawind.gitparcel.server.mc.storage.shared;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.leawind.gitparcel.server.mc.storage.shared.SharedContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedContentTest {
  private FileSystem fs;
  private Path tempDir;
  private SharedContent sharedContent;

  @BeforeEach
  void setUp() throws IOException {
    fs = Jimfs.newFileSystem();
    tempDir = fs.getPath("/shared");
    Files.createDirectories(tempDir);
    sharedContent = new SharedContent(tempDir);
  }

  @AfterEach
  void afterEach() throws IOException {
    fs.close();
  }

  @Test
  void testGetRoot() {
    assertEquals(tempDir, sharedContent.getRoot());
  }

  @Test
  void testGetReposIndexFile() {
    assertEquals(tempDir.resolve("repos.json"), sharedContent.getReposIndexFile());
  }

  @Test
  void testGetRepoDir() {
    assertEquals(tempDir.resolve("my_repo"), sharedContent.getRepoDir("my_repo"));
  }

  @Test
  void testLoadReposIndexWhenFileNotExists() throws IOException {
    var repos = sharedContent.loadReposIndex();
    assertTrue(repos.isEmpty());
  }

  @Test
  void testSaveAndLoadReposIndexRoundTrip() throws IOException {
    var info = SharedContent.RepoInfo.local();
    var expected = Map.of("my_repo", info);
    sharedContent.saveReposIndex(expected);

    var actual = sharedContent.loadReposIndex();
    assertEquals(1, actual.size());
    assertTrue(actual.containsKey("my_repo"));
    assertEquals("local", actual.get("my_repo").type());
    assertNull(actual.get("my_repo").remoteUrl());
    assertNull(actual.get("my_repo").lastSync());
  }

  @Test
  void testSaveAndLoadMultipleRepos() throws IOException {
    var repos =
        Map.of(
            "repo_a", SharedContent.RepoInfo.local(),
            "repo_b", SharedContent.RepoInfo.cloned("https://github.com/example/repo"));
    sharedContent.saveReposIndex(repos);

    var actual = sharedContent.loadReposIndex();
    assertEquals(2, actual.size());
    assertEquals("local", actual.get("repo_a").type());
    assertEquals("cloned", actual.get("repo_b").type());
    assertEquals("https://github.com/example/repo", actual.get("repo_b").remoteUrl());
    assertNotNull(actual.get("repo_b").lastSync());
  }

  @Test
  void testLoadRepoMetaWhenFileNotExists() throws IOException {
    var parcels = sharedContent.loadRepoMeta("nonexistent");
    assertTrue(parcels.isEmpty());
  }

  @Test
  void testSaveAndLoadRepoMetaRoundTrip() throws IOException {
    var expected = List.of("parcel_a/", "subdir/parcel_b/");
    sharedContent.saveRepoMeta("my_repo", expected);

    var actual = sharedContent.loadRepoMeta("my_repo");
    assertEquals(expected, actual);
  }

  @Test
  void testSaveAndLoadEmptyRepoMeta() throws IOException {
    var expected = List.<String>of();
    sharedContent.saveRepoMeta("my_repo", expected);

    var actual = sharedContent.loadRepoMeta("my_repo");
    assertTrue(actual.isEmpty());
  }

  @Test
  void testRepoMetaFileIsCreatedUnderRepoDir() throws IOException {
    sharedContent.saveRepoMeta("my_repo", List.of("parcel_a/"));

    var metaFile = tempDir.resolve("my_repo/meta.json");
    assertTrue(Files.exists(metaFile));
  }

  @Test
  void testReposIndexFileIsCreated() throws IOException {
    sharedContent.saveReposIndex(Map.of("repo", SharedContent.RepoInfo.local()));

    var indexFile = tempDir.resolve("repos.json");
    assertTrue(Files.exists(indexFile));
  }

  @Test
  void testRepoInfoLocalFactory() {
    var info = SharedContent.RepoInfo.local();
    assertEquals("local", info.type());
    assertNull(info.remoteUrl());
    assertNull(info.lastSync());
  }

  @Test
  void testRepoInfoClonedFactory() {
    var info = SharedContent.RepoInfo.cloned("https://github.com/test/repo");
    assertEquals("cloned", info.type());
    assertEquals("https://github.com/test/repo", info.remoteUrl());
    assertNotNull(info.lastSync());
  }
}
