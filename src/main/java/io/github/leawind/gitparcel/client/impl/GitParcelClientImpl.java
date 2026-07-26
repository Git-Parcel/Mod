package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.platform.ClientServices;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;

public final class GitParcelClientImpl implements GitParcelClient {
  public static final GitParcelClientImpl INSTANCE = new GitParcelClientImpl();

  private GitParcelClientImpl() {}

  private volatile ParcelFormatCapabilities capabilities = ParcelFormatCapabilities.empty();
  private final Parcels parcels = new Parcels();
  private volatile List<SharedRepositorySnapshot> sharedRepositories = List.of();
  private volatile List<OperationSnapshot> operations = List.of();
  private volatile Optional<String> sharedRepositoriesError = Optional.empty();
  private volatile Optional<String> operationsError = Optional.empty();
  private final ConcurrentHashMap<UUID, SnapshotTreePage> snapshotTrees =
      new ConcurrentHashMap<>();

  @Override
  public ParcelFormatCapabilities getParcelFormatCapabilities() {
    return capabilities;
  }

  @Override
  public Parcels getParcels() {
    return parcels;
  }

  @Override
  public List<SharedRepositorySnapshot> getSharedRepositories() {
    return sharedRepositories;
  }

  @Override
  public List<OperationSnapshot> getOperations() {
    return operations;
  }

  @Override
  public Optional<String> getSharedRepositoriesError() {
    return sharedRepositoriesError;
  }

  @Override
  public Optional<String> getOperationsError() {
    return operationsError;
  }

  @Override
  public Optional<SnapshotTreePage> getSnapshotTreePage(UUID parcelUuid) {
    return Optional.ofNullable(snapshotTrees.get(parcelUuid));
  }

  @Override
  public void queryServerState() {
    ClientServices.networking().send(QueryServerStateMessage.all());
  }

  @Override
  public void querySnapshotTree(UUID parcelUuid, Optional<SnapshotId> cursor, int limit) {
    ClientServices.networking()
        .send(new QueryParcelHistoryMessage(parcelUuid, cursor, limit));
  }

  public void setParcelFormatCapabilities(@NonNull ParcelFormatCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  public void setSharedRepositories(UpdateSharedRepositoriesMessage message) {
    sharedRepositories = message.repositories();
    sharedRepositoriesError = message.error();
  }

  public void setOperations(UpdateOperationsMessage message) {
    operations = message.operations();
    operationsError = message.error();
  }

  public void setParcelHistory(UpdateParcelHistoryMessage message) {
    snapshotTrees.put(message.page().parcelUuid(), message.page());
  }

  public void clearParcelHistory() {
    snapshotTrees.clear();
  }

  public void removeParcelHistory(Set<UUID> parcelUuids) {
    parcelUuids.forEach(snapshotTrees::remove);
  }

  public void reset() {
    capabilities = ParcelFormatCapabilities.empty();
    parcels.clear();
    sharedRepositories = List.of();
    operations = List.of();
    sharedRepositoriesError = Optional.empty();
    operationsError = Optional.empty();
    snapshotTrees.clear();
  }
}
