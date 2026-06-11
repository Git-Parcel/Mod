package io.github.leawind.gitparcel.common.api;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.impl.parcel.ParcelFormatRegistryImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Factory {
  private Factory() {}

  @Deprecated
  static GitParcelClient getGitParcelClient() {
    return GitParcelClientImpl.INSTANCE;
  }

  public static ParcelFormatRegistry getParcelFormatRegistry() {
    return ParcelFormatRegistryImpl.INSTANCE;
  }
}
