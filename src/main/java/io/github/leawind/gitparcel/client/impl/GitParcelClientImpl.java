package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import org.jspecify.annotations.NonNull;

public final class GitParcelClientImpl implements GitParcelClient {
  public static final GitParcelClientImpl INSTANCE = new GitParcelClientImpl();

  private final Parcels parcels = new Parcels();

  private GitParcelClientImpl() {}

  @Override
  public Parcels getParcels() {
    return parcels;
  }

  public void reset() {
    parcels.clear();
  }
}
