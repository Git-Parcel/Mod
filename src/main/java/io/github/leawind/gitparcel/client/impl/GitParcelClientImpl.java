package io.github.leawind.gitparcel.client.impl;

import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.platform.ClientServices;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import java.util.List;
import java.util.Optional;
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
  public void queryServerState() {
    ClientServices.networking().send(QueryServerStateMessage.all());
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

  public void reset() {
    capabilities = ParcelFormatCapabilities.empty();
    parcels.clear();
    sharedRepositories = List.of();
    gitOperations = List.of();
    sharedRepositoriesError = Optional.empty();
    gitOperationsError = Optional.empty();
  }
}
