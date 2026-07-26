package io.github.leawind.gitparcel.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import java.util.List;
import java.util.Optional;
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

    assertEquals(List.of(repository), client.getSharedRepositories());
    assertEquals(List.of(operation), client.getGitOperations());
    assertTrue(client.getGitOperationsError().isPresent());

    client.reset();

    assertTrue(client.getSharedRepositories().isEmpty());
    assertTrue(client.getGitOperations().isEmpty());
    assertTrue(client.getSharedRepositoriesError().isEmpty());
    assertTrue(client.getGitOperationsError().isEmpty());
  }
}
