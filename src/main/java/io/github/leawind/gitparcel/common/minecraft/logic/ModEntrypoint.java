package io.github.leawind.gitparcel.common.minecraft.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentCapabilities;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.impl.extension.GitParcelExtensions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelContentsMessage;
import io.github.leawind.gitparcel.common.platform.api.CommandArgumentTypeRegistrar;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parceldebug.ParcelDebugCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.ParcelsCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ParcelSynchronization;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ServerQueryHandler;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class ModEntrypoint {
  private ModEntrypoint() {}

  public static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Initializes the Git Parcel mod.
   *
   * <p>Called both on the client and server sides.
   */
  public static void initialize() {
    LOGGER.debug("Initializing");

    GitParcelExtensions.discoverAndFreeze();
  }

  /** Synchronizes server-owned registries and parcel state after a player enters play state. */
  public static void onPlayerJoin(ServerPlayer player) {
    var capabilities = ParcelContentCapabilities.from(ParcelContentTypeRegistry.get());
    Services.SERVER_NETWORKING.send(player, new UpdateParcelContentsMessage(capabilities));

    ParcelSynchronization.syncParcelsTo(player);
    ServerQueryHandler.syncAvailableState(player);
  }

  /** Replaces client parcel state after the player moves to another dimension. */
  public static void onPlayerChangeDimension(ServerPlayer player) {
    ParcelSynchronization.syncParcelsTo(player);
  }

  /** Audits parcel repositories and reports interrupted restore operations. */
  public static void onServerStarted(MinecraftServer server) {
    var manager = OperationManager.get(server);
    for (var level : server.getAllLevels()) {
      var parcels = ParcelRegistry.get(level).parcels();
      manager.submit(
          "audit_repositories",
          level.dimension().identifier().toString(),
          "server",
          () -> {
            SnapshotService.get(level).auditRepositories(parcels);
            return "Audited " + parcels.size() + " parcel repositories";
          },
          ignored -> {});
    }
  }

  /** Cancels queued work and releases operation worker threads for this server. */
  public static void onServerStopping(MinecraftServer server) {
    ServerQueryHandler.shutdown(server);
    OperationManager.shutdown(server);
  }

  public static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext context,
      Commands.CommandSelection commandSelection) {
    LOGGER.debug("Registering commands");

    ParcelsCommand.register(dispatcher, context);
    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }

  public static void registerCommandArgumentTypes(CommandArgumentTypeRegistrar registrar) {
    LOGGER.debug("Registering command argument types");

    registrar.register(
        "file_path",
        FilePathArgument.class,
        SingletonArgumentInfo.contextFree(FilePathArgument::new));

    registrar.register("parcel", ParcelArgument.class, new ParcelArgument.Info());
  }
}
