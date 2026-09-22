package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftVersion;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.server.MinecraftServer;

/**
 * JSON API routes for the web console.
 *
 * <p>Every route that reads server state collects it on the server thread via {@link
 * MinecraftServer#submit} and waits a bounded time; the HTTP thread never touches Minecraft state
 * directly.
 */
final class WebApi implements WebService.ApiHandler {
  /** Upper bound for a server-thread collection pass before the request fails with 503. */
  private static final long SERVER_CALLBACK_TIMEOUT_SECONDS = 10;

  private static final Gson GSON = new Gson();

  private final MinecraftServer server;

  WebApi(MinecraftServer server) {
    this.server = server;
  }

  @Override
  public Optional<WebService.WebResponse> handle(
      String method, String path, Map<String, String> query) {
    if (!"GET".equals(method)) {
      return Optional.empty();
    }
    if (!"/api/status".equals(path)) {
      return Optional.empty();
    }
    try {
      var json = server.submit(this::collectStatus)
          .get(SERVER_CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      return Optional.of(WebService.WebResponse.json(200, json));
    } catch (TimeoutException e) {
      return Optional.of(WebService.WebResponse.jsonError(503, "server_busy"));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.of(WebService.WebResponse.jsonError(503, "server_busy"));
    } catch (ExecutionException e) {
      return Optional.of(WebService.WebResponse.jsonError(500, "internal_error"));
    }
  }

  private String collectStatus() {
    var minecraft = new JsonObject();
    minecraft.addProperty("name", MinecraftVersion.currentVersionName());
    minecraft.addProperty("dataVersion", MinecraftVersion.currentDataVersion());

    var gitparcel = new JsonObject();
    gitparcel.addProperty("version", WebConsoleMeta.modVersion());

    var parcels = new JsonArray();
    for (var level : server.getAllLevels()) {
      var entry = new JsonObject();
      /*? if >=26.1 {*/
      entry.addProperty("dimension", level.dimension().identifier().toString());
      /*?} else {*/
      /*entry.addProperty("dimension", level.dimension().location().toString());
       *//*? }*/
      entry.addProperty("count", ParcelRegistry.get(level).parcels().size());
      parcels.add(entry);
    }

    var operations = new JsonObject();
    var recent = OperationManager.get(server).recent(100);
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
    return GSON.toJson(root);
  }
}
