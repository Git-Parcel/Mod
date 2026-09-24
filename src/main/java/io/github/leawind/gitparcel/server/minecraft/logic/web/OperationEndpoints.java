package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebService.WebResponse;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import java.util.Map;
import java.util.UUID;

/** Handlers for the operation progress endpoints. */
final class OperationEndpoints {
  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 100;

  private OperationEndpoints() {}

  static WebResponse list(WebApi api, Map<String, String> query) {
    var limit = parseLimit(query.getOrDefault("limit", String.valueOf(DEFAULT_LIMIT)));
    var operations = new JsonArray();
    for (var snapshot : api.manager().recent(limit)) {
      operations.add(ParcelJson.operation(snapshot));
    }
    var body = new JsonObject();
    body.add("operations", operations);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  static WebResponse get(WebApi api, String rawUuid) {
    UUID uuid;
    try {
      uuid = UUID.fromString(rawUuid);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
    var snapshot =
        api.manager()
            .get(uuid)
            .orElseThrow(() -> new ApiException(404, "not_found"));
    return WebResponse.json(200, ApiJson.GSON.toJson(ParcelJson.operation(snapshot)));
  }

  /**
   * Resumes an interrupted restore: retry re-applies the target snapshot,
   * rollback re-applies the pre-restore snapshot. Mirrors
   * {@code /parcel restore recover}.
   */
  static WebResponse recover(WebApi api, String rawUuid, JsonObject request) {
    UUID operationId;
    try {
      operationId = UUID.fromString(rawUuid);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
    var action = ApiJson.requireString(request, "action");
    var rollback =
        switch (action) {
          case "retry" -> false;
          case "rollback" -> true;
          default -> throw new ApiException(400, "invalid_value");
        };

    var source = api.manager().get(operationId);
    if (
      source.isEmpty()
        || !"restore_snapshot".equals(source.get().kind())
        || source.get().state() != OperationSnapshot.State.FAILED
    ) {
      // Only a failed restore can be resumed; recovering anything else (or a
      // restore that already succeeded) is a client error.
      throw new ApiException(400, "invalid_value");
    }
    var targetUuid = source.get().target();

    var ref = api.requireParcel(targetUuid);
    var service = api.onServerThread(() -> SnapshotService.get(ref.level()));
    return api.submitOperation(
        rollback ? "rollback_restore" : "retry_restore",
        ref.parcel().uuid().toString(),
        progress ->
            service
                .resolvePendingRestoreInBackground(
                    ref.parcel(), operationId, rollback, false, progress, api.manager())
                .restored()
                .value());
  }

  private static int parseLimit(String raw) {
    int limit;
    try {
      limit = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      throw new ApiException(400, "invalid_value");
    }
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new ApiException(400, "invalid_value");
    }
    return limit;
  }
}
