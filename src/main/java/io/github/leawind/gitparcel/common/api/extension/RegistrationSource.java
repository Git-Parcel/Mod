package io.github.leawind.gitparcel.common.api.extension;

/**
 * Identifies the extension that registered an entry, carrying the rule 7.4 adjudication inputs.
 *
 * @param extensionId The {@link GitParcelExtension#id()} that registered the entry.
 * @param owner Whether the extension declares ownership of the entry's namespace.
 * @param priority Explicit tie-break priority among same-tier registrations.
 */
public record RegistrationSource(String extensionId, boolean owner, int priority) {
  public RegistrationSource {
    if (extensionId == null || extensionId.isBlank()) {
      throw new IllegalArgumentException("Registration source must name its extension");
    }
  }

  /**
   * Rule 7.4 adjudication: owners beat guests regardless of load order; within one tier the higher
   * priority wins, and equal priorities fall back to the lexicographically smaller extension id.
   */
  public boolean supersedes(RegistrationSource existing) {
    if (owner != existing.owner) {
      return owner;
    }
    if (priority != existing.priority) {
      return priority > existing.priority;
    }
    return extensionId.compareTo(existing.extensionId) < 0;
  }
}
