package io.github.leawind.gitparcel.common.utils.git;

/**
 * Independently enforceable Git capabilities exposed by an owning repository service.
 *
 * <p>Every value must have at least one enforcing consumer; capabilities for unimplemented
 * operations (branches, tags, history rewrite) return together with those operations.
 */
public enum RepositoryCapability {
  READ_HISTORY,
  READ_TREE,
  CREATE_COMMIT,
  UPDATE_MANAGED_REFS,
  /** Maintaining a working tree and index, including staging and resetting paths. */
  WORKTREE,
  /** Contacting configured remotes through clone, fetch, pull, or push. */
  REMOTES,
  MULTI_PARENT_COMMITS
}
