package io.github.leawind.gitparcel.core.api;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Bridge {
  private Bridge() {}

  @Deprecated
  static GitParcelClient getGitParcelClient() {
    return GitParcelClientImpl.INSTANCE;
  }
}
