package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.ParcelFormatSpecs;
import org.jspecify.annotations.NonNull;

public final class GitParcelClientImpl implements GitParcelClient {
  public static GitParcelClientImpl INSTANCE = new GitParcelClientImpl();

  private GitParcelClientImpl() {}

  private volatile ParcelFormatSpecs specs = ParcelFormatSpecs.empty();
  public volatile Parcels parcels = new Parcels();

  @Override
  public ParcelFormatSpecs getParcelFormatSpecs() {
    return specs;
  }

  @Override
  public Parcels getParcels() {
    return parcels;
  }

  public void setParcelFormatSpecs(@NonNull ParcelFormatSpecs specs) {
    this.specs = specs;
  }
}
