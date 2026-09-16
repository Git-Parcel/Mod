package io.github.leawind.gitparcel.client.api;

import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.world.Parcels;

public interface GitParcelClient {
  /**
   * Parcels in the server world.
   *
   * <p>Updated when any parcel is updated on the server.
   */
  Parcels getParcels();

  static GitParcelClient get() {
    return GitParcelClientImpl.INSTANCE;
  }
}
