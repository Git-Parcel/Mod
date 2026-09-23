package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebService.WebResponse;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebApi.ParcelRef;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import io.github.leawind.gitparcel.server.minecraft.logic.world.PublishImportService;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/** Handlers for the parcel lifecycle, snapshot and import endpoints. */
final class ParcelEndpoints {
  private static final String DEFAULT_SNAPSHOT_NAME = "Snapshot";
  private static final String DEFAULT_PUBLISH_MESSAGE = "Publish parcel snapshot";

  private ParcelEndpoints() {}

  static WebResponse list(WebApi api, Map<String, String> query) {
    var filter = query.get("dimension");
    var parcels =
        api.onServerThread(
            () -> {
              var array = new JsonArray();
              for (ServerLevel level : api.server().getAllLevels()) {
                var dimension = ParcelJson.dimensionId(level);
                if (filter != null && !filter.isEmpty() && !filter.equals(dimension)) {
                  continue;
                }
                for (Parcel parcel : ParcelRegistry.get(level).parcels()) {
                  array.add(ParcelJson.parcel(parcel, dimension));
                }
              }
              return array;
            });
    var body = new JsonObject();
    body.add("parcels", parcels);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  static WebResponse get(WebApi api, String uuid) {
    var ref = api.requireParcel(uuid);
    var json =
        api.onServerThread(
            () -> ParcelJson.parcel(ref.parcel(), ParcelJson.dimensionId(ref.level())));
    return WebResponse.json(200, ApiJson.GSON.toJson(json));
  }

  static WebResponse create(WebApi api, JsonObject request) {
    var dimension = ApiJson.requireString(request, "dimension");
    var from = ApiJson.requireCoord(request, "from");
    var to = ApiJson.requireCoord(request, "to");
    var name = ApiJson.requireString(request, "name");
    var mirror = ApiJson.optEnum(request, "mirror", Mirror.class).orElse(Mirror.NONE);
    var rotation = ApiJson.optEnum(request, "rotation", Rotation.class).orElse(Rotation.NONE);

    var json =
        api.onServerThread(
            () -> {
              var level = api.requireLevel(dimension);
              var registry = ParcelRegistry.get(level);
              var box =
                  BoundingBox.fromCorners(
                      new BlockPos(from[0], from[1], from[2]),
                      new BlockPos(to[0], to[1], to[2]));
              var permissions =
                  GitParcelWorldSavedData.get(api.server())
                      .parcelDefaultPermissions()
                      .copy();
              var parcel = ParcelFactory.create(box, mirror, rotation, permissions);
              try {
                parcel.meta().setName(name);
                registry.addNewParcel(parcel);
              } catch (IllegalArgumentException e) {
                throw validationError(e);
              }
              return ParcelJson.parcel(parcel, ParcelJson.dimensionId(level));
            });
    return WebResponse.json(201, ApiJson.GSON.toJson(json));
  }

  static WebResponse batchDelete(WebApi api, JsonObject request) {
    var uuids = parseUuids(request);
    var count =
        api.onServerThread(
            () -> {
              var deleted = 0;
              for (ServerLevel level : api.server().getAllLevels()) {
                var registry = ParcelRegistry.get(level);
                var targets = new ArrayList<Parcel>();
                for (var uuid : uuids) {
                  var parcel = registry.getParcel(uuid);
                  if (parcel != null) {
                    targets.add(parcel);
                  }
                }
                if (!targets.isEmpty()) {
                  deleted += registry.deleteParcels(targets);
                }
              }
              return deleted;
            });
    var body = new JsonObject();
    body.addProperty("count", count);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  static WebResponse delete(WebApi api, String uuid) {
    var ref = api.requireParcel(uuid);
    api.onServerThread(
        () -> {
          try {
            if (ParcelRegistry.get(ref.level()).deleteParcel(ref.parcel().uuid()) == null) {
              throw new ApiException(404, "not_found");
            }
          } catch (ParcelException.Busy e) {
            throw new ApiException(409, "busy");
          }
          return null;
        });
    return WebResponse.json(204, "");
  }

  static WebResponse config(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    var key = ApiJson.requireString(request, "key");
    var json =
        api.onServerThread(
            () -> {
              var parcel = ref.parcel();
              applyConfig(parcel, key, request);
              ParcelRegistry.get(ref.level()).updateParcel(parcel);
              return ParcelJson.parcel(parcel, ParcelJson.dimensionId(ref.level()));
            });
    return WebResponse.json(200, ApiJson.GSON.toJson(json));
  }

  static WebResponse resize(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    var from = ApiJson.requireCoord(request, "from");
    var to = ApiJson.requireCoord(request, "to");
    var json =
        api.onServerThread(
            () -> {
              var box =
                  BoundingBox.fromCorners(
                      new BlockPos(from[0], from[1], from[2]),
                      new BlockPos(to[0], to[1], to[2]));
              ParcelRegistry.get(ref.level()).resizeParcel(ref.parcel(), box);
              return ParcelJson.parcel(ref.parcel(), ParcelJson.dimensionId(ref.level()));
            });
    return WebResponse.json(200, ApiJson.GSON.toJson(json));
  }

  static WebResponse save(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    var name =
        ApiJson.optString(request, "name")
            .filter(value -> !value.isBlank())
            .orElse(DEFAULT_SNAPSHOT_NAME);
    var service = api.onServerThread(() -> SnapshotService.get(ref.level()));
    return api.submitOperation(
        "save_snapshot",
        ref.parcel().uuid().toString(),
        progress ->
            service
                .saveSnapshotInBackground(
                    ref.parcel(), name, "", WebApi.WEB_IDENTITY, false, progress, api.manager())
                .value());
  }

  static WebResponse restore(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    SnapshotId snapshot;
    try {
      snapshot = new SnapshotId(ApiJson.requireString(request, "snapshotId"));
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
    var mode = parseRestoreMode(request);
    var service = api.onServerThread(() -> SnapshotService.get(ref.level()));
    return api.submitOperation(
        "restore_snapshot",
        ref.parcel().uuid().toString(),
        progress -> {
          service.restoreSnapshotInBackground(
              ref.parcel(), snapshot, mode, false, WebApi.WEB_IDENTITY, progress, api.manager());
          return snapshot.value();
        });
  }

  static WebResponse history(WebApi api, String uuid, Map<String, String> query) {
    var ref = api.requireParcel(uuid);
    var limit = parseLimit(query.getOrDefault("limit", "20"));
    var cursor = parseCursor(query.get("cursor"));
    var service = api.onServerThread(() -> SnapshotService.get(ref.level()));
    SnapshotTreePage page;
    try {
      page = service.querySnapshotTreeInBackground(ref.parcel(), limit, cursor, api.manager());
    } catch (IOException e) {
      throw staleOrInternal(e);
    } catch (Exception e) {
      throw WebApi.unwrap(e);
    }
    return WebResponse.json(200, ApiJson.GSON.toJson(ParcelJson.treePage(page)));
  }

  static WebResponse teleport(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    var players = parseUuids(request);
    var count =
        api.onServerThread(
            () -> {
              var position = Vec3.atBottomCenterOf(ref.parcel().getBoundingBox().getCenter());
              var teleported = 0;
              for (var playerUuid : players) {
                var player = api.server().getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                  player.teleportTo(position.x, position.y, position.z);
                  teleported++;
                }
              }
              return teleported;
            });
    var body = new JsonObject();
    body.addProperty("count", count);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  static WebResponse publish(WebApi api, String uuid, JsonObject request) {
    var ref = api.requireParcel(uuid);
    var repository = ApiJson.requireString(request, "repository");
    var path = ApiJson.requireString(request, "path");
    var message =
        ApiJson.optString(request, "message")
            .filter(value -> !value.isBlank())
            .orElse(DEFAULT_PUBLISH_MESSAGE);
    var service = api.onServerThread(() -> PublishImportService.get(ref.level()));
    return api.submitOperation(
        "publish_snapshot",
        repository + ":" + path,
        progress ->
            service
                .publishCurrentSnapshotInBackground(
                    ref.parcel(), repository, path, message, WebApi.WEB_IDENTITY, progress,
                    api.manager())
                .revision());
  }

  static WebResponse importParcel(WebApi api, JsonObject request) {
    var dimension = ApiJson.requireString(request, "dimension");
    var repository = ApiJson.requireString(request, "repository");
    var revision = ApiJson.requireString(request, "revision");
    var path = ApiJson.requireString(request, "path");
    var at = ApiJson.requireCoord(request, "at");
    var mirror = ApiJson.optEnum(request, "mirror", Mirror.class).orElse(Mirror.NONE);
    var rotation = ApiJson.optEnum(request, "rotation", Rotation.class).orElse(Rotation.NONE);

    var level = api.requireLevel(dimension);
    var service = api.onServerThread(() -> PublishImportService.get(level));
    var transform =
        new ParcelTransform(mirror, rotation, new BlockPos(at[0], at[1], at[2]));
    return api.submitOperation(
        "import_snapshot",
        repository + "@" + revision + ":" + path,
        progress ->
            service
                .importSharedSnapshotInBackground(
                    repository, revision, path, transform, WebApi.WEB_IDENTITY, progress,
                    api.manager())
                .uuid()
                .toString());
  }

  static WebResponse players(WebApi api) {
    var players =
        api.onServerThread(
            () -> {
              var array = new JsonArray();
              for (var player : api.server().getPlayerList().getPlayers()) {
                array.add(ParcelJson.player(player));
              }
              return array;
            });
    var body = new JsonObject();
    body.add("players", players);
    return WebResponse.json(200, ApiJson.GSON.toJson(body));
  }

  private static RestoreSnapshotRequest.Mode parseRestoreMode(JsonObject request) {
    var mode = ApiJson.optString(request, "mode").orElse("direct");
    return switch (mode) {
      case "direct" -> RestoreSnapshotRequest.Mode.DIRECT;
      case "save-first" -> RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE;
      default -> throw new ApiException(400, "invalid_value");
    };
  }

  private static void applyConfig(Parcel parcel, String key, JsonObject request) {
    switch (key) {
      case "meta.name" -> {
        var value = ApiJson.requireString(request, "value");
        try {
          parcel.meta().setName(value);
        } catch (IllegalArgumentException e) {
          throw new ApiException(400, "invalid_name");
        }
      }
      case "meta.author" -> parcel.meta().setAuthor(ApiJson.requireString(request, "value"));
      case "meta.description" ->
          parcel.meta().setDescription(ApiJson.requireString(request, "value"));
      case "meta.excludeEntities" ->
          parcel.meta().setExcludeEntities(ApiJson.requireBool(request, "value"));
      case "visual.showWireframe" ->
          parcel.visual().showWireframe(ApiJson.requireBool(request, "value"));
      case "visual.showAnchor" ->
          parcel.visual().showAnchor(ApiJson.requireBool(request, "value"));
      case "content.blocks.sectionSize" -> setSectionSize(parcel, request);
      default -> throw new ApiException(400, "invalid_value");
    }
  }

  private static void setSectionSize(Parcel parcel, JsonObject request) {
    var value = request.get("value");
    if (value == null
        || !value.isJsonPrimitive()
        || !value.getAsJsonPrimitive().isNumber()
        || (value.getAsInt() != 16 && value.getAsInt() != 32)) {
      throw new ApiException(400, "invalid_value");
    }
    var sectionSize =
        value.getAsInt() == 16
            ? BlockContentType.BlockSectionSize.SIZE_16
            : BlockContentType.BlockSectionSize.SIZE_32;
    var config = new BlockContentType.Config();
    var previous = parcel.meta().contents().get(BlockContentType.ID);
    if (previous != null && previous.config() != null && previous.config().isJsonObject()) {
      config.setFromJson(previous.config().getAsJsonObject());
    }
    config.sectionSize.set(sectionSize);
    var contents = new java.util.LinkedHashMap<>(parcel.meta().contents());
    contents.put(
        BlockContentType.ID,
        new io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest(
            BlockContentType.SPEC.version(), config.toJson()));
    parcel.meta().setContents(contents);
  }

  /** Classifies creation validation failures by their stable message prefixes. */
  private static ApiException validationError(IllegalArgumentException e) {
    var message = e.getMessage() == null ? "" : e.getMessage();
    if (message.startsWith("Parcel is too big")) {
      return new ApiException(400, "volume_limit");
    }
    if (message.contains("intersects")) {
      return new ApiException(400, "overlap");
    }
    if (message.startsWith("Invalid name")) {
      return new ApiException(400, "invalid_name");
    }
    return new ApiException(400, "invalid_value");
  }

  private static List<UUID> parseUuids(JsonObject request) {
    var array = request.get("players");
    if (array == null || !array.isJsonArray()) {
      throw new ApiException(400, "invalid_body");
    }
    var result = new ArrayList<UUID>();
    for (JsonElement element : array.getAsJsonArray()) {
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
        throw new ApiException(400, "invalid_body");
      }
      try {
        result.add(UUID.fromString(element.getAsString()));
      } catch (IllegalArgumentException e) {
        throw new ApiException(400, "invalid_value");
      }
    }
    return result;
  }

  private static int parseLimit(String raw) {
    int limit;
    try {
      limit = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      throw new ApiException(400, "invalid_value");
    }
    if (limit < 1 || limit > 100) {
      throw new ApiException(400, "invalid_value");
    }
    return limit;
  }

  private static java.util.Optional<SnapshotId> parseCursor(String raw) {
    if (raw == null || raw.isBlank()) {
      return java.util.Optional.empty();
    }
    try {
      return java.util.Optional.of(new SnapshotId(raw));
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
  }

  private static ApiException staleOrInternal(IOException e) {
    var message = e.getMessage() == null ? "" : e.getMessage();
    if (message.contains("stale") || message.contains("cursor")) {
      return new ApiException(400, "stale_cursor");
    }
    return new ApiException(500, "internal_error");
  }
}
