package io.github.leawind.gitparcel.common.utils.git;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Capability policy checked by {@link GitRepositoryCore}, independently of command visibility. */
public final class RepositoryPolicy {
  public static final RepositoryPolicy INTERNAL =
      new RepositoryPolicy(
          EnumSet.of(
              RepositoryCapability.READ_HISTORY,
              RepositoryCapability.READ_TREE,
              RepositoryCapability.CREATE_COMMIT,
              RepositoryCapability.UPDATE_MANAGED_REFS));

  public static final RepositoryPolicy SHARED =
      new RepositoryPolicy(EnumSet.allOf(RepositoryCapability.class));

  private final Set<RepositoryCapability> capabilities;

  public RepositoryPolicy(Set<RepositoryCapability> capabilities) {
    if (capabilities == null || capabilities.isEmpty()) {
      this.capabilities = Collections.emptySet();
    } else {
      this.capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }
  }

  public Set<RepositoryCapability> capabilities() {
    return capabilities;
  }

  public boolean permits(RepositoryCapability capability) {
    return capabilities.contains(capability);
  }

  public void require(RepositoryCapability capability) {
    if (!permits(capability)) {
      throw new SecurityException("Repository policy denies " + capability);
    }
  }
}
