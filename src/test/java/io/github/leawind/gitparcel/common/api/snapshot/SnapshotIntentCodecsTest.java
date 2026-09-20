package io.github.leawind.gitparcel.common.api.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SnapshotIntentCodecsTest {
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
            .parse(JsonOps.INSTANCE, SaveSnapshotRequest.CODEC.encodeStart(JsonOps.INSTANCE, save).result().orElseThrow())
            .result().orElseThrow());
    assertEquals(
        restore,
        RestoreSnapshotRequest.CODEC
            .parse(
                JsonOps.INSTANCE,
                RestoreSnapshotRequest.CODEC.encodeStart(JsonOps.INSTANCE, restore).result().orElseThrow())
            .result().orElseThrow());
    assertEquals(
        result,
        SnapshotOperationResult.CODEC
            .parse(
                JsonOps.INSTANCE,
                SnapshotOperationResult.CODEC.encodeStart(JsonOps.INSTANCE, result).result().orElseThrow())
            .result().orElseThrow());
    assertThrows(IllegalArgumentException.class, () -> new SnapshotId("HEAD~1"));
  }
}
