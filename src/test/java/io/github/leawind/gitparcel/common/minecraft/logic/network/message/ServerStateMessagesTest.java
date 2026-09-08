package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.api.snapshot.SaveSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotOperationResult;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
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
        new OperationSnapshot(
            UUID.randomUUID(),
            "pull",
            "Tester",
            "builds",
            OperationSnapshot.State.FAILED,
            "failed",
            5,
            Optional.of(10L),
            Optional.of("objects"),
            "2026-07-26T00:00:00Z",
            Optional.of("2026-07-26T00:00:01Z"),
            "2026-07-26T00:00:02Z",
            Optional.of("2026-07-26T00:00:02Z"),
            Optional.empty(),
            Optional.of("Non-fast-forward"),
            Optional.of(io.github.leawind.gitparcel.common.api.operation.OperationErrorCode.INTERNAL));
    var expected =
        new UpdateOperationsMessage(
            List.of(operation), Optional.of("example error"));
    var json =
        UpdateOperationsMessage.CODEC
            .encodeStart(JsonOps.INSTANCE, expected)
            .getOrThrow();
    var actual =
        UpdateOperationsMessage.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(expected, actual);
    assertTrue(actual.error().isPresent());
  }

  @Test
  void historyQueryAndPageCodecsPreserveCursor() {
    UUID parcelUuid = UUID.randomUUID();
    var cursor = new SnapshotId("a".repeat(40));
    var next = new SnapshotId("b".repeat(40));
    var query = new QueryParcelHistoryMessage(parcelUuid, Optional.of(cursor), 20);
    var queryJson =
        QueryParcelHistoryMessage.CODEC.encodeStart(JsonOps.INSTANCE, query).getOrThrow();
    assertEquals(
        query,
        QueryParcelHistoryMessage.CODEC.parse(JsonOps.INSTANCE, queryJson).getOrThrow());

    var page =
        new SnapshotTreePage(
            parcelUuid,
            Optional.of(cursor),
            List.of(
                new SnapshotNode(
                    cursor,
                    Optional.empty(),
                    "Snapshot",
                    "Description",
                    "author",
                    "2026-07-26T00:00:00Z",
                    SnapshotNode.Source.SAVED,
                    new SnapshotNode.ContentSummary(2, 10))),
            Optional.of(cursor),
            Optional.of(next),
            Optional.empty());
    var message = new UpdateParcelHistoryMessage(page);
    var pageJson =
        UpdateParcelHistoryMessage.CODEC.encodeStart(JsonOps.INSTANCE, message).getOrThrow();
    assertEquals(
        message,
        UpdateParcelHistoryMessage.CODEC.parse(JsonOps.INSTANCE, pageJson).getOrThrow());
  }

  @Test
  void snapshotIntentAndResultCodecsUseOpaqueObjectIds() {
    UUID parcelUuid = UUID.randomUUID();
    UUID operationId = UUID.randomUUID();
    var snapshotId = new SnapshotId("c".repeat(40));
    var save = new SaveSnapshotRequest(parcelUuid, "House", "Second floor", false);
    var restore =
        new RestoreSnapshotRequest(
            parcelUuid,
            snapshotId,
            RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE,
            true);
    var result =
        new SnapshotOperationResult(
            operationId,
            SnapshotOperationResult.Status.SUCCEEDED,
            Optional.of(snapshotId),
            SnapshotOperationResult.ErrorCategory.NONE,
            "Saved");

    assertEquals(
        save,
        SaveSnapshotRequest.CODEC
            .parse(JsonOps.INSTANCE, SaveSnapshotRequest.CODEC.encodeStart(JsonOps.INSTANCE, save).getOrThrow())
            .getOrThrow());
    assertEquals(
        restore,
        RestoreSnapshotRequest.CODEC
            .parse(
                JsonOps.INSTANCE,
                RestoreSnapshotRequest.CODEC.encodeStart(JsonOps.INSTANCE, restore).getOrThrow())
            .getOrThrow());
    assertEquals(
        result,
        SnapshotOperationResult.CODEC
            .parse(
                JsonOps.INSTANCE,
                SnapshotOperationResult.CODEC.encodeStart(JsonOps.INSTANCE, result).getOrThrow())
            .getOrThrow());
    assertThrows(IllegalArgumentException.class, () -> new SnapshotId("HEAD~1"));
  }
}
