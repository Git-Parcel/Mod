package io.github.leawind.gitparcel.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateOperationsMessage;
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
        new OperationSnapshot(
            UUID.randomUUID(),
            "create",
            "Tester",
            "builds",
            OperationSnapshot.State.SUCCEEDED,
            "succeeded",
            1,
            Optional.of(1L),
            Optional.of("repositories"),
            "2026-07-26T00:00:00Z",
            Optional.empty(),
            "2026-07-26T00:00:01Z",
            Optional.of("2026-07-26T00:00:01Z"),
            Optional.of("Created"),
            Optional.empty(),
            Optional.empty());

    client.setSharedRepositories(
        new UpdateSharedRepositoriesMessage(List.of(repository), Optional.empty()));
    client.setOperations(
        new UpdateOperationsMessage(List.of(operation), Optional.of("stale warning")));
    UUID parcelUuid = UUID.randomUUID();
    client.setParcelHistory(
        new UpdateParcelHistoryMessage(
            new SnapshotTreePage(
                parcelUuid,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())));

    assertEquals(List.of(repository), client.getSharedRepositories());
    assertEquals(List.of(operation), client.getOperations());
    assertTrue(client.getOperationsError().isPresent());
    assertTrue(client.getSnapshotTreePage(parcelUuid).isPresent());

    client.removeParcelHistory(Set.of(parcelUuid));
    assertTrue(client.getSnapshotTreePage(parcelUuid).isEmpty());
    client.setParcelHistory(
        new UpdateParcelHistoryMessage(
            new SnapshotTreePage(
                parcelUuid,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty())));

    client.reset();

    assertTrue(client.getSharedRepositories().isEmpty());
    assertTrue(client.getOperations().isEmpty());
    assertTrue(client.getSharedRepositoriesError().isEmpty());
    assertTrue(client.getOperationsError().isEmpty());
    assertTrue(client.getSnapshotTreePage(parcelUuid).isEmpty());
  }
}
