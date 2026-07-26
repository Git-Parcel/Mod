package io.github.leawind.gitparcel.common.utils.git;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Provider-independent validation and resolution for untrusted portable tree paths. */
public final class SafeSnapshotPath {
  private static final Set<String> WINDOWS_RESERVED =
      Set.of(
          "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6",
          "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6",
          "lpt7", "lpt8", "lpt9");

  private SafeSnapshotPath() {}

  public static String validateGitPath(String value, int maxDepth) {
    if (value == null
        || value.isEmpty()
        || value.startsWith("/")
        || value.endsWith("/")
        || value.contains("\\")
        || value.contains("//")
        || value.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Invalid portable snapshot path: " + value);
    }
    String[] parts = value.split("/", -1);
    if (parts.length > maxDepth) {
      throw new IllegalArgumentException("Snapshot path is too deep: " + value);
    }
    for (String part : parts) {
      validatePart(part, value);
    }
    return value;
  }

  public static String toGitPath(Path root, Path file, int maxDepth) {
    Path relative = root.relativize(file);
    if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.getNameCount() > maxDepth) {
      throw new IllegalArgumentException("Path is outside snapshot root: " + file);
    }
    var result = new StringBuilder();
    for (Path part : relative) {
      if (!result.isEmpty()) {
        result.append('/');
      }
      String name = part.toString();
      validatePart(name, relative.toString());
      result.append(name);
    }
    return validateGitPath(result.toString(), maxDepth);
  }

  public static Path resolve(Path root, String gitPath, int maxDepth) {
    String validated = validateGitPath(gitPath, maxDepth);
    Path result = root;
    for (String part : validated.split("/")) {
      result = result.resolve(part);
    }
    Path normalized = result.normalize();
    if (!normalized.startsWith(root.normalize())) {
      throw new IllegalArgumentException("Snapshot path escapes target root: " + gitPath);
    }
    return normalized;
  }

  private static void validatePart(String part, String wholePath) {
    String lower = part.toLowerCase(Locale.ROOT);
    int dot = lower.indexOf('.');
    String stem = dot < 0 ? lower : lower.substring(0, dot);
    if (part.isEmpty()
        || part.equals(".")
        || part.equals("..")
        || lower.equals(".git")
        || lower.endsWith(".lock")
        || part.endsWith(" ")
        || part.endsWith(".")
        || WINDOWS_RESERVED.contains(stem)
        || !Normalizer.isNormalized(part, Normalizer.Form.NFC)
        || part.chars().anyMatch(character -> Character.isISOControl(character))
        || part.chars().anyMatch(character -> "<>:\"|?*".indexOf(character) >= 0)
        || part.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("Unsafe snapshot path: " + wholePath);
    }
  }
}
