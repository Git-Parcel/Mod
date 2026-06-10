package io.github.leawind.gitparcel.mc.client;

import io.github.leawind.gitparcel.core.world.Parcels;
import io.github.leawind.gitparcel.mc.network.protocol.parcelformat.ParcelFormatSpecs;
import io.github.leawind.gitparcel.mc.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;

@Deprecated
public final class GitParcelClient {

  /**
   * Cache of the parcel format specs received from the server.
   *
   * <p>Updated when received {@link UpdateParcelFormatSpecS2CPayload}.
   *
   * <p>Better be set to null when the client disconnects from the server.
   */
  public static volatile ParcelFormatSpecs PARCEL_FORMAT_SPECS = ParcelFormatSpecs.empty();

  public static volatile Parcels PARCELS = new Parcels();
}
