package io.github.leawind.gitparcel.permission;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class GitParcelPermission {
  public static final Pattern NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

  public static void validateName(@Nullable String name) {
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }

    // Check name
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "name must match pattern " + NAME_PATTERN.pattern() + ", got: " + name);
    }
  }
}
