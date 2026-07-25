package io.github.leawind.gitparcel.client.api;

import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;

public interface GitParcelClient {
  /**
   * Parcel format specs supported by the server.
   *
   * <p>Automatically updated when joining a server.
   */
  ParcelFormatCapabilities getParcelFormatCapabilities();

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
