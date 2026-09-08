package io.github.leawind.gitparcel.server.minecraft.logic.network;

import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.operation.OperationErrorCode;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
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

    var parcel = ParcelRegistry.get(player.level()).getParcel(request.parcelUuid());
    if (parcel == null) {
      sendHistoryFailure(player, request, "Parcel not found");
      return;
    }
    if (!MinecraftPermissions.permits(
        player, parcel.permissions(), ParcelPermissions.VIEW)) {
      sendHistoryFailure(player, request, "Permission denied");
      return;
    }

    var service = SnapshotService.get(player.level());
    var manager = OperationManager.get(player.level().getServer());
    var result = new AtomicReference<SnapshotTreePage>();
    manager.submit(
        "query_snapshot_tree",
        parcel.uuid().toString(),
        player.getUUID().toString(),
        ignored -> {
          var page =
              service.querySnapshotTreeInBackground(
                  parcel, request.limit(), request.cursor(), manager);
          result.set(page);
          return page.nodes().size() + " snapshot nodes";
        },
        completed -> {
          if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
            Services.SERVER_NETWORKING.send(
                player, new UpdateParcelHistoryMessage(result.get()));
          } else {
            sendHistoryFailure(
                player,
                request,
                completed.errorCode().filter(OperationErrorCode.QUEUE_FULL::equals).isPresent()
                    ? "Server is busy; try again shortly"
                    : "Failed to read parcel history");
          }
        });
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
    boolean canManage =
        MinecraftPermissions.permits(
            player, permissions, WorldPermissions.MANAGE_SHARED_REPOSITORIES);
    String playerId = player.getUUID().toString();
    var operations = OperationManager.get(server).recent(OPERATION_LIMIT).stream()
        .filter(operation -> canManage || operation.owner().equals(playerId))
        .toList();
    Services.SERVER_NETWORKING.send(
        player, new UpdateOperationsMessage(operations, Optional.empty()));
  }

  private static void sendHistoryFailure(
      ServerPlayer player, QueryParcelHistoryMessage request, String error) {
    Services.SERVER_NETWORKING.send(
        player,
        new UpdateParcelHistoryMessage(
            SnapshotTreePage.failure(request.parcelUuid(), request.cursor(), error)));
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
