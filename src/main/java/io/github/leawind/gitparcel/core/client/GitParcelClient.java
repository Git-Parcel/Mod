package io.github.leawind.gitparcel.core.client;

import io.github.leawind.gitparcel.core.network.protocol.parcelformat.ParcelFormatSpecs;
import io.github.leawind.gitparcel.core.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.core.world.Parcels;
import org.jspecify.annotations.Nullable;

@Deprecated
public final class GitParcelClient {

  /**
   * Cache of the parcel format specs received from the server.
   *
   * <p>Updated when received {@link UpdateParcelFormatSpecS2CPayload}.
   *
   * <p>Better be set to null when the client disconnects from the server.
   */
  public static @Nullable volatile ParcelFormatSpecs PARCEL_FORMAT_SPECS = null;

  public static volatile Parcels PARCELS = new Parcels();
}
