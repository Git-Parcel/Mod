package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.platform.ClientServices;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.ParcelHistoryPage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
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
  private volatile List<GitOperationSnapshot> gitOperations = List.of();
  private volatile Optional<String> sharedRepositoriesError = Optional.empty();
  private volatile Optional<String> gitOperationsError = Optional.empty();
  private final ConcurrentHashMap<UUID, ParcelHistoryPage> parcelHistory =
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
  public List<GitOperationSnapshot> getGitOperations() {
    return gitOperations;
  }

  @Override
  public Optional<String> getSharedRepositoriesError() {
    return sharedRepositoriesError;
  }

  @Override
  public Optional<String> getGitOperationsError() {
    return gitOperationsError;
  }

  @Override
  public Optional<ParcelHistoryPage> getParcelHistoryPage(UUID parcelUuid) {
    return Optional.ofNullable(parcelHistory.get(parcelUuid));
  }

  @Override
  public void queryServerState() {
    ClientServices.networking().send(QueryServerStateMessage.all());
  }

  @Override
  public void queryParcelHistory(
      UUID parcelUuid, Optional<String> beforeRevision, int limit) {
    ClientServices.networking()
        .send(new QueryParcelHistoryMessage(parcelUuid, beforeRevision, limit));
  }

  public void setParcelFormatCapabilities(@NonNull ParcelFormatCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  public void setSharedRepositories(UpdateSharedRepositoriesMessage message) {
    sharedRepositories = message.repositories();
    sharedRepositoriesError = message.error();
  }

  public void setGitOperations(UpdateGitOperationsMessage message) {
    gitOperations = message.operations();
    gitOperationsError = message.error();
  }

  public void setParcelHistory(UpdateParcelHistoryMessage message) {
    parcelHistory.put(message.page().parcelUuid(), message.page());
  }

  public void clearParcelHistory() {
    parcelHistory.clear();
  }

  public void removeParcelHistory(Set<UUID> parcelUuids) {
    parcelUuids.forEach(parcelHistory::remove);
  }

  public void reset() {
    capabilities = ParcelFormatCapabilities.empty();
    parcels.clear();
    sharedRepositories = List.of();
    gitOperations = List.of();
    sharedRepositoriesError = Optional.empty();
    gitOperationsError = Optional.empty();
    parcelHistory.clear();
  }
}
