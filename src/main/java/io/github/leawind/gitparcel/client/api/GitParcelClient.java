package io.github.leawind.gitparcel.client.api;

import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import java.util.List;
import java.util.Optional;

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

  /** Most recently received shared repository snapshots. */
  List<SharedRepositorySnapshot> getSharedRepositories();

  /** Most recently received asynchronous Git operation snapshots. */
  List<GitOperationSnapshot> getGitOperations();

  Optional<String> getSharedRepositoriesError();

  Optional<String> getGitOperationsError();

  /** Requests fresh repository and Git-operation state from the server. */
  void queryServerState();

  static GitParcelClient get() {
    return GitParcelClientImpl.INSTANCE;
  }
}
