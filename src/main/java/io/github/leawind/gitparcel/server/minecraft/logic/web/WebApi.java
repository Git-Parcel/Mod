package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftVersion;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebService.WebResponse;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON API dispatcher for the web console.
 *
 * <p>Routes are resolved by {@link ApiRouter}; handlers live in {@link ParcelEndpoints}, {@link
 * OperationEndpoints} and {@link RepositoryEndpoints}. Every server-state access goes through
 * {@link #onServerThread} with a bounded wait, and background mutations are submitted through
 * {@link OperationManager} exactly like their command counterparts.
 */
public final class WebApi implements WebService.ApiHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebApi.class);

  /** Upper bound for a server-thread collection pass before the request fails with 503. */
  private static final long SERVER_CALLBACK_TIMEOUT_SECONDS = 10;

  /** Owner recorded on operations submitted through the web console. */
  public static final String WEB_OWNER = "web-console";

  /** Git identity used for snapshots saved through the web console. */
  public static final GitRepositoryCore.Identity WEB_IDENTITY =
      new GitRepositoryCore.Identity("Web Console", "web-console@gitparcel.local");

  private final MinecraftServer server;

  public WebApi(MinecraftServer server) {
    this.server = server;
  }

  @Override
  public Optional<WebResponse> handle(Request request) {
    var match = ApiRouter.match(request.method(), request.path());
    if (match.isEmpty()) {
      return Optional.empty();
    }
    try {
      var body = ApiJson.parseObject(request.body());
      return Optional.of(dispatch(match.get(), body, request.query()));
    } catch (ApiException e) {
      return Optional.of(WebResponse.jsonError(e.status(), e.code()));
    } catch (Exception e) {
      LOGGER.error("Web API request failed: {} {}", request.method(), request.path(), e);
      return Optional.of(WebResponse.jsonError(500, "internal_error"));
    }
  }

  MinecraftServer server() {
    return server;
  }

  OperationManager manager() {
    return OperationManager.get(server);
  }

  /** Runs a state-reading action on the server thread with a bounded wait. */
  <T> T onServerThread(Callable<T> action) {
    try {
      return server
          .submit(() -> wrapCallable(action))
          .get(SERVER_CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new ApiException(503, "server_busy");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(503, "server_busy");
    } catch (ExecutionException e) {
      throw unwrap(e.getCause());
    }
  }

  private static <T> T wrapCallable(Callable<T> action) {
    try {
      return action.call();
    } catch (Exception e) {
      throw new WrappedActionException(e);
    }
  }

  private static final class WrappedActionException extends RuntimeException {
    private WrappedActionException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Submits a background operation owned by the web console and returns it as a 202 response.
   * Rejection at submit time (full queue, shutdown) surfaces as 503.
   */
  WebResponse submitOperation(String kind, String target, OperationManager.OperationAction action) {
    OperationSnapshot operation;
    try {
      operation = manager().submit(kind, target, WEB_OWNER, action, ignored -> {});
    } catch (Exception e) {
      LOGGER.error("Web API failed to submit operation {} for {}", kind, target, e);
      throw new ApiException(503, "server_busy");
    }
    if (operation.state() == OperationSnapshot.State.FAILED) {
      throw new ApiException(503, "server_busy");
    }
    return WebResponse.json(202, ApiJson.GSON.toJson(ParcelJson.operation(operation)));
  }

  /** A parcel together with the level whose registry owns it. */
  record ParcelRef(Parcel parcel, ServerLevel level) {}

  ParcelRef requireParcel(String rawUuid) {
    java.util.UUID uuid;
    try {
      uuid = java.util.UUID.fromString(rawUuid);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
    for (ServerLevel level : server.getAllLevels()) {
      var parcel = ParcelRegistry.get(level).getParcel(uuid);
      if (parcel != null) {
        return new ParcelRef(parcel, level);
      }
    }
    throw new ApiException(404, "not_found");
  }

  ServerLevel requireLevel(String dimension) {
    var parts = dimension.split(":", 2);
    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
      throw new ApiException(400, "invalid_value");
    }
    var key = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(parts[0], parts[1]));
    var level = server.getLevel(key);
    if (level == null) {
      throw new ApiException(404, "not_found");
    }
    return level;
  }

  /** Maps a use-case failure thrown on a worker thread to its API error response. */
  static ApiException unwrap(Throwable cause) {
    while (cause instanceof WrappedActionException) {
      cause = cause.getCause();
    }
    if (cause == null) {
      return new ApiException(500, "internal_error");
    }
    if (cause instanceof ApiException apiException) {
      return apiException;
    }
    if (cause instanceof io.github.leawind.gitparcel.common.api.exceptions.ParcelException.Busy) {
      return new ApiException(409, "busy");
    }
    if (cause instanceof IllegalArgumentException) {
      return new ApiException(400, "invalid_value");
    }
    LOGGER.error("Web API use case failed", cause);
    return new ApiException(500, "internal_error");
  }

  private WebResponse dispatch(ApiRouter.Match match, JsonObject body, Map<String, String> query) {
    var params = match.params();
    return switch (match.route()) {
      case STATUS -> status();
      case PARCELS_LIST -> ParcelEndpoints.list(this, query);
      case PARCELS_CREATE -> ParcelEndpoints.create(this, body);
      case PARCELS_BATCH_DELETE -> ParcelEndpoints.batchDelete(this, body);
      case PARCEL_GET -> ParcelEndpoints.get(this, params.get("uuid"));
      case PARCEL_DELETE -> ParcelEndpoints.delete(this, params.get("uuid"));
      case PARCEL_CONFIG -> ParcelEndpoints.config(this, params.get("uuid"), body);
      case PARCEL_RESIZE -> ParcelEndpoints.resize(this, params.get("uuid"), body);
      case PARCEL_SAVE -> ParcelEndpoints.save(this, params.get("uuid"), body);
      case PARCEL_HISTORY -> ParcelEndpoints.history(this, params.get("uuid"), query);
      case PARCEL_RESTORE -> ParcelEndpoints.restore(this, params.get("uuid"), body);
      case PARCEL_TELEPORT -> ParcelEndpoints.teleport(this, params.get("uuid"), body);
      case PARCEL_PUBLISH -> ParcelEndpoints.publish(this, params.get("uuid"), body);
      case IMPORT -> ParcelEndpoints.importParcel(this, body);
      case PLAYERS -> ParcelEndpoints.players(this);
      case OPERATIONS_LIST -> OperationEndpoints.list(this, query);
      case OPERATION_GET -> OperationEndpoints.get(this, params.get("uuid"));
      case REPOSITORIES_LIST -> RepositoryEndpoints.list(this);
      case REPOSITORY_PATHS -> RepositoryEndpoints.paths(this, params.get("name"), query);
      case REPOSITORY_CREATE -> RepositoryEndpoints.create(this, body);
      case REPOSITORY_CLONE -> RepositoryEndpoints.cloneRepository(this, params.get("name"), body);
      case REPOSITORY_FETCH -> RepositoryEndpoints.fetch(this, params.get("name"));
      case REPOSITORY_PULL -> RepositoryEndpoints.pull(this, params.get("name"));
      case REPOSITORY_PUSH -> RepositoryEndpoints.push(this, params.get("name"));
    };
  }

  private WebResponse status() {
    var json = onServerThread(this::collectStatus);
    return WebResponse.json(200, json);
  }

  private String collectStatus() {
    var minecraft = new JsonObject();
    minecraft.addProperty("name", MinecraftVersion.currentVersionName());
    minecraft.addProperty("dataVersion", MinecraftVersion.currentDataVersion());

    var gitparcel = new JsonObject();
    gitparcel.addProperty("version", WebConsoleMeta.modVersion());

    var parcels = new com.google.gson.JsonArray();
    for (var level : server.getAllLevels()) {
      var entry = new JsonObject();
      entry.addProperty("dimension", ParcelJson.dimensionId(level));
      entry.addProperty("count", ParcelRegistry.get(level).parcels().size());
      parcels.add(entry);
    }

    var operations = new JsonObject();
    var recent = manager().recent(100);
    var active =
        recent.stream()
            .filter(
                operation ->
                    operation.state() == OperationSnapshot.State.QUEUED
                        || operation.state() == OperationSnapshot.State.RUNNING)
            .count();
    operations.addProperty("active", active);
    operations.addProperty("retained", recent.size());

    var root = new JsonObject();
    root.add("minecraft", minecraft);
    root.add("gitparcel", gitparcel);
    root.addProperty("onlinePlayers", server.getPlayerCount());
    root.addProperty("maxPlayers", server.getMaxPlayers());
    root.add("parcels", parcels);
    root.add("operations", operations);
    root.addProperty("serverTime", Instant.now().toString());
    return ApiJson.GSON.toJson(root);
  }
}
