package io.github.leawind.gitparcel.client.api;

import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.ParcelFormatSpecs;

public interface GitParcelClient {
  /**
   * Parcel format specs supported by the server.
   *
   * <p>Automatically updated when joining a server.
   */
  ParcelFormatSpecs getParcelFormatSpecs();

  /**
   * Parcels in the server world.
   *
   * <p>Updated when any parcel is updated on the server.
   */
  Parcels getParcels();
}
