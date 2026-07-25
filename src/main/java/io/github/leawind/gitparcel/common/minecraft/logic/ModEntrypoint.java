package io.github.leawind.gitparcel.common.minecraft.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.impl.extension.GitParcelExtensions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelFormatsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.platform.api.CommandArgumentTypeRegistrar;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parceldebug.ParcelDebugCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.ParcelsCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
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
    var capabilities = ParcelFormatCapabilities.from(ParcelFormatRegistry.get());
    Services.SERVER_NETWORKING.send(player, new UpdateParcelFormatsMessage(capabilities));

    ParcelService.get(player.level()).syncTo(player);
  }

  /** Replaces client parcel state after the player moves to another dimension. */
  public static void onPlayerChangeDimension(ServerPlayer player) {
    ParcelService.get(player.level()).syncTo(player);
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

    registrar.register(
        "parcel_format_writer",
        ParcelFormatArgument.Writer.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::writer));

    registrar.register(
        "parcel_format_reader",
        ParcelFormatArgument.Reader.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::reader));

    registrar.register("parcel", ParcelArgument.class, new ParcelArgument.Info());
  }
}
