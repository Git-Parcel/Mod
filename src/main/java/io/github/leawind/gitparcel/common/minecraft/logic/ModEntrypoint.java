package io.github.leawind.gitparcel.common.minecraft.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.mvp.MvpFormat;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Loader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.structuretemplate.StructureTemplateFormat;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
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

    registerFormats();
  }

  private static void registerFormats() {
    ParcelFormatRegistry.get().registerDefaultSaver(new ParcellaD32Saver());
    ParcelFormatRegistry.get().register(new ParcellaD32Loader());
    ParcelFormatRegistry.get().register(new ParcellaD16Loader());

    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      ParcelFormatRegistry.get().register(new StructureTemplateFormat());
      ParcelFormatRegistry.get().register(new ParcellaD16Saver());
      ParcelFormatRegistry.get().register(new MvpFormat());
    }
  }

  /** Synchronizes server-owned registries and parcel state after a player enters play state. */
  public static void onPlayerJoin(ServerPlayer player) {
    var formatSpecs = UpdateParcelFormatSpecS2CPayload.from(ParcelFormatRegistry.get());
    Services.SERVER_NETWORKING.send(player, formatSpecs);

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
        "parcel_format_saver",
        ParcelFormatArgument.Saver.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::saver));

    registrar.register(
        "parcel_format_loader",
        ParcelFormatArgument.Loader.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::loader));

    registrar.register("parcel", ParcelArgument.class, new ParcelArgument.Info());
  }
}
