package io.github.leawind.gitparcel.common.api.permission;

/**
 * Loader- and Minecraft-version-independent command permission levels.
 *
 * <p>The numeric ids intentionally match vanilla's traditional operator levels so persisted
 * permission configs remain compatible across Minecraft permission API changes.
 */
public enum PermissionLevel {
  ALL(0),
  MODERATORS(1),
  GAMEMASTERS(2),
  ADMINS(3),
  OWNERS(4);

  private static final PermissionLevel[] VALUES = values();

  private final int id;

  PermissionLevel(int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }

  /** Resolves an id using vanilla-compatible clamping for out-of-range values. */
  public static PermissionLevel byId(int id) {
    return VALUES[Math.max(0, Math.min(id, VALUES.length - 1))];
  }

  /** Returns whether this granted level includes the required level. */
  public boolean includes(PermissionLevel required) {
    return id >= required.id;
  }
}
