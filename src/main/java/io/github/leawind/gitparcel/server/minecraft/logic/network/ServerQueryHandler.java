package io.github.leawind.gitparcel.server.minecraft.logic.network;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
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
  private static final Map<MinecraftServer, Map<UUID, Long>> LAST_QUERIES =
      new WeakHashMap<>();

  private ServerQueryHandler() {}

  public static void handle(QueryServerStateMessage request, ServerPlayer player) {
    if (!acceptQuery(player)) {
      return;
    }
    if (request.repositories()) {
      syncRepositories(player);
    }
    if (request.operations()) {
      syncOperations(player);
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

  private static synchronized boolean acceptQuery(ServerPlayer player) {
    var server = player.level().getServer();
    long now = System.nanoTime();
    var queries = LAST_QUERIES.computeIfAbsent(server, ignored -> new HashMap<>());
    Long previous = queries.get(player.getUUID());
    if (previous != null && now - previous < QUERY_COOLDOWN_NANOS) {
      return false;
    }
    queries.put(player.getUUID(), now);
    return true;
  }
}
