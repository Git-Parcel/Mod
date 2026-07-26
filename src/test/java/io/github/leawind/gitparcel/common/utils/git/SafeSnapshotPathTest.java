package io.github.leawind.gitparcel.common.utils.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SafeSnapshotPathTest {
  @Test
  void acceptsAndResolvesPortablePaths() {
    Path root = Path.of("workspace");

    assertEquals(
        root.resolve("data/region/r.0.0.parcella"),
        SafeSnapshotPath.resolve(root, "data/region/r.0.0.parcella", 8));
  }

  @Test
  void rejectsTraversalGitAndPlatformAmbiguities() {
    for (String path :
        new String[] {
          "../outside",
          "/absolute",
          "data/.git/config",
          "data/CON.txt",
          "data/trailing.",
          "data/colon:name",
          "data/question?.txt",
          "data/control\u0001.txt",
          "data/e\u0301.txt"
        }) {
      assertThrows(
          IllegalArgumentException.class,
          () -> SafeSnapshotPath.validateGitPath(path, 8),
          path);
    }
  }
}
