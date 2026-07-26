package io.github.leawind.gitparcel.common.utils.git;

/** Independently enforceable Git capabilities exposed by an owning repository service. */
public enum RepositoryCapability {
  READ_HISTORY,
  READ_TREE,
  CREATE_COMMIT,
  UPDATE_MANAGED_REFS,
  WORKTREE,
  BRANCHES,
  TAGS,
  REMOTES,
  MULTI_PARENT_COMMITS,
  HISTORY_REWRITE
}
