package io.github.leawind.gitparcel.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.ParcelHistoryPage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GitParcelClientImplTest {
  private final GitParcelClientImpl client = GitParcelClientImpl.INSTANCE;

  @AfterEach
  void resetClient() {
    client.reset();
  }

  @Test
  void replacesStructuredServerStateAndClearsItOnDisconnect() {
    var repository =
        new SharedRepositorySnapshot(
            "builds", "local", Optional.empty(), Optional.empty(), List.of("house"));
    var operation =
        new GitOperationSnapshot(
            1,
            "create",
            "builds",
            "Tester",
            "SUCCEEDED",
            "2026-07-26T00:00:00Z",
            Optional.empty(),
            Optional.of("2026-07-26T00:00:01Z"),
            "Created");

    client.setSharedRepositories(
        new UpdateSharedRepositoriesMessage(List.of(repository), Optional.empty()));
    client.setGitOperations(
        new UpdateGitOperationsMessage(List.of(operation), Optional.of("stale warning")));
    UUID parcelUuid = UUID.randomUUID();
    client.setParcelHistory(
        new UpdateParcelHistoryMessage(
            new ParcelHistoryPage(
                parcelUuid,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty())));

    assertEquals(List.of(repository), client.getSharedRepositories());
    assertEquals(List.of(operation), client.getGitOperations());
    assertTrue(client.getGitOperationsError().isPresent());
    assertTrue(client.getParcelHistoryPage(parcelUuid).isPresent());

    client.removeParcelHistory(Set.of(parcelUuid));
    assertTrue(client.getParcelHistoryPage(parcelUuid).isEmpty());
    client.setParcelHistory(
        new UpdateParcelHistoryMessage(
            new ParcelHistoryPage(
                parcelUuid,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty())));

    client.reset();

    assertTrue(client.getSharedRepositories().isEmpty());
    assertTrue(client.getGitOperations().isEmpty());
    assertTrue(client.getSharedRepositoriesError().isEmpty());
    assertTrue(client.getGitOperationsError().isEmpty());
    assertTrue(client.getParcelHistoryPage(parcelUuid).isEmpty());
  }
}
