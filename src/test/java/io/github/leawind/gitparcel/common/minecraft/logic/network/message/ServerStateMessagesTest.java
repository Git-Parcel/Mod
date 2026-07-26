package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.git.GitCommitSnapshot;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.ParcelHistoryPage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  @Test
  void historyQueryAndPageCodecsPreserveCursor() {
    UUID parcelUuid = UUID.randomUUID();
    var query = new QueryParcelHistoryMessage(parcelUuid, Optional.of("cursor"), 20);
    var queryJson =
        QueryParcelHistoryMessage.CODEC.encodeStart(JsonOps.INSTANCE, query).getOrThrow();
    assertEquals(
        query,
        QueryParcelHistoryMessage.CODEC.parse(JsonOps.INSTANCE, queryJson).getOrThrow());

    var page =
        new ParcelHistoryPage(
            parcelUuid,
            Optional.of("cursor"),
            List.of(new GitCommitSnapshot("revision", "time", "author", "message")),
            Optional.of("next"),
            Optional.empty());
    var message = new UpdateParcelHistoryMessage(page);
    var pageJson =
        UpdateParcelHistoryMessage.CODEC.encodeStart(JsonOps.INSTANCE, message).getOrThrow();
    assertEquals(
        message,
        UpdateParcelHistoryMessage.CODEC.parse(JsonOps.INSTANCE, pageJson).getOrThrow());
  }
}
