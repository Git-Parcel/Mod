package io.github.leawind.gitparcel.mc.entrypoint;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.core.parcelformats.mvp.MvpFormat;
import io.github.leawind.gitparcel.core.parcelformats.parcella.d16.ParcellaD16Loader;
import io.github.leawind.gitparcel.core.parcelformats.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.core.parcelformats.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.core.parcelformats.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.core.parcelformats.structuretemplate.StructureTemplateFormat;
import io.github.leawind.gitparcel.mc.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.mc.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.mc.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.mc.mixin.InvokeArgumentTypeInfos;
import io.github.leawind.gitparcel.mc.network.protocol.parcelformat.UpdateParcelFormatSpecS2CPayload;
import io.github.leawind.gitparcel.mc.network.protocol.parcels.UpdateParcelsS2CPayload;
import io.github.leawind.gitparcel.mc.platform.api.Services;
import io.github.leawind.gitparcel.mc.server.GameServerApi;
import io.github.leawind.gitparcel.mc.server.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.mc.server.commands.parceldebug.ParcelDebugCommand;
import io.github.leawind.gitparcel.mc.server.commands.parcels.ParcelsCommand;
import io.github.leawind.gitparcel.mc.world.GitParcelLevelSavedData;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.slf4j.Logger;

///
public final class ModEntrypoint {
  public static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Initializes the Git Parcel mod.
   *
   * <p>Called both on the client and server sides.
   */
  public static void initialize() {
    LOGGER.debug("Initializing");

    registerFormats();

    GameServerApi.ON_PLAYER_JOIN.on(
        e -> {
          var payload = UpdateParcelFormatSpecS2CPayload.from(ParcelFormatRegistry.INSTANCE);
          var packet = new ClientboundCustomPayloadPacket(payload);
          e.player().connection.send(packet);
        });

    registerGitParcelEvents();
  }

  private static void registerFormats() {
    ParcelFormatRegistry.INSTANCE.registerDefaultSaver(new ParcellaD32Saver());
    ParcelFormatRegistry.INSTANCE.register(new ParcellaD32Loader());
    ParcelFormatRegistry.INSTANCE.register(new ParcellaD16Loader());

    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      ParcelFormatRegistry.INSTANCE.register(new StructureTemplateFormat());
      ParcelFormatRegistry.INSTANCE.register(new ParcellaD16Saver());
      ParcelFormatRegistry.INSTANCE.register(new MvpFormat());
    }
  }

  private static void registerGitParcelEvents() {
    // Notify when player join
    GameServerApi.ON_PLAYER_JOIN.on(
        e -> {
          var player = e.player();
          var parcels = GitParcelLevelSavedData.get(player.level()).parcels();
          var payload = UpdateParcelsS2CPayload.fullSync(parcels);
          player.connection.send(new ClientboundCustomPayloadPacket(payload));
        });
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

  public static void registerCommandArgumentTypes(Registry<ArgumentTypeInfo<?, ?>> registry) {
    LOGGER.debug("Registering command argument types");

    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:file_path",
        FilePathArgument.class,
        SingletonArgumentInfo.contextFree(FilePathArgument::new));

    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:parcel_format_saver",
        ParcelFormatArgument.Saver.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::saver));

    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:parcel_format_loader",
        ParcelFormatArgument.Loader.class,
        SingletonArgumentInfo.contextFree(ParcelFormatArgument::loader));

    InvokeArgumentTypeInfos.register(
        registry, "gitparcel:parcel", ParcelArgument.class, new ParcelArgument.Info());
  }
}
