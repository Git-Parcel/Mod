package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServerStateMessagesTest {
  @Test
  void queryCodecRoundTripsRequestedSections() {
    var expected = new QueryServerStateMessage(true, false);
    var json = QueryServerStateMessage.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();

    assertEquals(expected, QueryServerStateMessage.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
  }

  @Test
  void repositoryCodecPreservesPathsAndOptionalMetadata() {
    var repository =
        new SharedRepositorySnapshot(
            "builds",
            "cloned",
            Optional.of("https://example.invalid/builds.git"),
            Optional.of("2026-07-26T00:00:00Z"),
            List.of("spawn/house", "spawn/tower"));
    var expected =
        new UpdateSharedRepositoriesMessage(List.of(repository), Optional.empty());
    var json =
        UpdateSharedRepositoriesMessage.CODEC
            .encodeStart(JsonOps.INSTANCE, expected)
            .getOrThrow();

    assertEquals(
        expected,
        UpdateSharedRepositoriesMessage.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
  }

  @Test
  void operationCodecPreservesLifecycleTimestampsAndErrors() {
    var operation =
        new GitOperationSnapshot(
            42,
            "pull",
            "builds",
            "Tester",
            "FAILED",
            "2026-07-26T00:00:00Z",
            Optional.of("2026-07-26T00:00:01Z"),
            Optional.of("2026-07-26T00:00:02Z"),
            "Non-fast-forward");
    var expected =
        new UpdateGitOperationsMessage(
            List.of(operation), Optional.of("example error"));
    var json =
        UpdateGitOperationsMessage.CODEC
            .encodeStart(JsonOps.INSTANCE, expected)
            .getOrThrow();
    var actual =
        UpdateGitOperationsMessage.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(expected, actual);
    assertTrue(actual.error().isPresent());
  }
}
