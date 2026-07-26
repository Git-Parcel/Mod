package io.github.leawind.gitparcel.client.api;

import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

  /** Most recently received permission-filtered asynchronous operations. */
  List<OperationSnapshot> getOperations();

  Optional<String> getSharedRepositoriesError();

  Optional<String> getOperationsError();

  /** Most recently received history page for a parcel. */
  Optional<SnapshotTreePage> getSnapshotTreePage(UUID parcelUuid);

  /** Requests fresh repository and operation state from the server. */
  void queryServerState();

  /** Requests one history page; pass an empty cursor for the newest page. */
  void querySnapshotTree(UUID parcelUuid, Optional<SnapshotId> cursor, int limit);

  static GitParcelClient get() {
    return GitParcelClientImpl.INSTANCE;
  }
}
