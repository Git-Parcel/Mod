package io.github.leawind.gitparcel.common.utils.git;

import java.io.IOException;

/** The repository path lock is held by a long-running operation; the access can be retried later. */
public class RepositoryBusyException extends IOException {
  public RepositoryBusyException(String message) {
    super(message);
  }
}
