package io.github.leawind.gitparcel.permission;

import io.github.leawind.gitparcel.GitParcelTranslations;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * @see PermissionLevel
 */
public record PermissionType(byte id, String name, byte defaultLevel) {
  public static final Pattern NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

  public PermissionType {
    // Check id
    if (id < 0 || id > 63) {
      throw new IllegalArgumentException("id must be in range [0, 63], got: " + id);
    }

    // Check name
    Objects.requireNonNull(name, "name cannot be null");
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "name must match pattern " + NAME_PATTERN.pattern() + ", got: " + name);
    }

    // Check defaultLevel
    if (defaultLevel < 0) {
      throw new IllegalArgumentException("defaultLevel must be >= 0, got: " + defaultLevel);
    }
    if (defaultLevel > 4) {
      throw new IllegalArgumentException("defaultLevel must be <= 4, got: " + defaultLevel);
    }
  }

  public long mask() {
    return 1L << id;
  }

  public Component translation() {
    return GitParcelTranslations.of("gitparcel.permission." + name);
  }
}
