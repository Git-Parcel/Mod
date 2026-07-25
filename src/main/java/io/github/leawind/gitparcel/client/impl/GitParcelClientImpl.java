package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import org.jspecify.annotations.NonNull;

public final class GitParcelClientImpl implements GitParcelClient {
  public static final GitParcelClientImpl INSTANCE = new GitParcelClientImpl();

  private GitParcelClientImpl() {}

  private volatile ParcelFormatCapabilities capabilities = ParcelFormatCapabilities.empty();
  private final Parcels parcels = new Parcels();

  @Override
  public ParcelFormatCapabilities getParcelFormatCapabilities() {
    return capabilities;
  }

  @Override
  public Parcels getParcels() {
    return parcels;
  }

  public void setParcelFormatCapabilities(@NonNull ParcelFormatCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  public void reset() {
    capabilities = ParcelFormatCapabilities.empty();
    parcels.clear();
  }
}
