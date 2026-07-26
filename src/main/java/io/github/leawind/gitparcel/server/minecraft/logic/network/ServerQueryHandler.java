package io.github.leawind.gitparcel.server.minecraft.logic.network;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.git.GitCommitSnapshot;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.ParcelHistoryPage;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.server.minecraft.logic.git.GitOperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Answers client state queries after applying server-side permissions. */
public final class ServerQueryHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final int OPERATION_LIMIT = 100;
  private static final long QUERY_COOLDOWN_NANOS = 500_000_000L;
  private static final Map<MinecraftServer, Map<QueryKey, Long>> LAST_QUERIES =
      new WeakHashMap<>();

  private ServerQueryHandler() {}

  public static void handle(QueryServerStateMessage request, ServerPlayer player) {
    if (!acceptQuery(player, "server_state")) {
      return;
    }
    if (request.repositories()) {
      syncRepositories(player);
    }
    if (request.operations()) {
      syncOperations(player);
    }
  }

  public static void handle(QueryParcelHistoryMessage request, ServerPlayer player) {
    if (!acceptQuery(player, "parcel_history")) {
      sendHistoryFailure(player, request, "Too many history requests");
      return;
    }

    var parcel = ParcelService.get(player.level()).getParcel(request.parcelUuid());
    if (parcel == null) {
      sendHistoryFailure(player, request, "Parcel not found");
      return;
    }
    if (!MinecraftPermissions.permits(
        player, parcel.permissions(), ParcelPermissions.LOAD)) {
      sendHistoryFailure(player, request, "Permission denied");
      return;
    }

    try {
      var page =
          ParcelService.get(player.level())
              .getParcelHistoryPage(
                  parcel, request.limit(), request.beforeRevision().orElse(null));
      var commits =
          page.commits().stream()
              .map(
                  commit ->
                      new GitCommitSnapshot(
                          commit.revision(),
                          commit.committedAt().toString(),
                          commit.author(),
                          commit.message()))
              .toList();
      Services.SERVER_NETWORKING.send(
          player,
          new UpdateParcelHistoryMessage(
              new ParcelHistoryPage(
                  request.parcelUuid(),
                  request.beforeRevision(),
                  commits,
                  page.nextCursor(),
                  Optional.empty())));
    } catch (Exception e) {
      LOGGER.error("Failed to query history for parcel {}", request.parcelUuid(), e);
      sendHistoryFailure(player, request, describeHistoryError(e));
    }
  }

  public static void syncAvailableState(ServerPlayer player) {
    syncRepositories(player);
    syncOperations(player);
  }

  public static synchronized void shutdown(MinecraftServer server) {
    LAST_QUERIES.remove(server);
  }

  public static void syncRepositories(ServerPlayer player) {
    var server = player.level().getServer();
    var permissions = GitParcelWorldSavedData.get(server).permissions();
    if (!MinecraftPermissions.permits(
        player, permissions, WorldPermissions.LIST_SHARED_REPOSITORIES)) {
      Services.SERVER_NETWORKING.send(
          player, new UpdateSharedRepositoriesMessage(List.of(), Optional.empty()));
      return;
    }

    try {
      var service = SharedRepositoryService.get(server);
      var repositories = new ArrayList<SharedRepositorySnapshot>();
      for (var entry : service.list().entrySet()) {
        var info = entry.getValue();
        repositories.add(
            new SharedRepositorySnapshot(
                entry.getKey(),
                info.type(),
                Optional.ofNullable(info.remoteUrl()),
                Optional.ofNullable(info.lastSync()),
                service.parcelPaths(entry.getKey())));
      }
      Services.SERVER_NETWORKING.send(
          player, new UpdateSharedRepositoriesMessage(repositories, Optional.empty()));
    } catch (Exception e) {
      LOGGER.error(
          "Failed to query shared repositories for {}", player.getGameProfile().name(), e);
      Services.SERVER_NETWORKING.send(
          player,
          UpdateSharedRepositoriesMessage.failure("Failed to read shared repositories"));
    }
  }

  public static void syncOperations(ServerPlayer player) {
    var server = player.level().getServer();
    var permissions = GitParcelWorldSavedData.get(server).permissions();
    if (!MinecraftPermissions.permits(
        player, permissions, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      Services.SERVER_NETWORKING.send(
          player, new UpdateGitOperationsMessage(List.of(), Optional.empty()));
      return;
    }

    var operations =
        GitOperationManager.get(server).recent(OPERATION_LIMIT).stream()
            .map(ServerQueryHandler::snapshot)
            .toList();
    Services.SERVER_NETWORKING.send(
        player, new UpdateGitOperationsMessage(operations, Optional.empty()));
  }

  private static GitOperationSnapshot snapshot(
      GitOperationManager.OperationSnapshot operation) {
    return new GitOperationSnapshot(
        operation.id(),
        operation.type(),
        operation.repository(),
        operation.requestedBy(),
        operation.status().name(),
        operation.submittedAt().toString(),
        Optional.ofNullable(operation.startedAt()).map(Object::toString),
        Optional.ofNullable(operation.completedAt()).map(Object::toString),
        operation.detail());
  }

  private static void sendHistoryFailure(
      ServerPlayer player, QueryParcelHistoryMessage request, String error) {
    Services.SERVER_NETWORKING.send(
        player,
        new UpdateParcelHistoryMessage(
            ParcelHistoryPage.failure(
                request.parcelUuid(), request.beforeRevision(), error)));
  }

  private static String describeHistoryError(Exception exception) {
    String message = exception.getMessage();
    if ("Shared repository is busy".equals(message)) {
      return message;
    }
    if (message != null
        && (message.startsWith("Unknown Git history cursor")
            || message.startsWith("Git history cursor does not belong"))) {
      return "Invalid or stale history cursor";
    }
    return "Failed to read parcel history";
  }

  private static synchronized boolean acceptQuery(ServerPlayer player, String category) {
    var server = player.level().getServer();
    long now = System.nanoTime();
    var queries = LAST_QUERIES.computeIfAbsent(server, ignored -> new HashMap<>());
    var key = new QueryKey(player.getUUID(), category);
    Long previous = queries.get(key);
    if (previous != null && now - previous < QUERY_COOLDOWN_NANOS) {
      return false;
    }
    queries.put(key, now);
    return true;
  }

  private record QueryKey(UUID playerUuid, String category) {}
}
