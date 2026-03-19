package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.GitParcelApi;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.commands.arguments.ParcelInstanceArgument;
import io.github.leawind.gitparcel.mixin.InvokeArgumentTypeInfos;
import io.github.leawind.gitparcel.network.protocol.parcelformat.UpdateParcelFormatInfosS2CPayload;
import io.github.leawind.gitparcel.network.protocol.parcelinstance.UpdateParcelInstancesS2CPayload;
import io.github.leawind.gitparcel.parcelformats.mvp.MvpFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Loader;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.parcelformats.structuretemplate.StructureTemplateFormat;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.GameServerApi;
import io.github.leawind.gitparcel.server.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.parcel_debug.ParcelDebugCommand;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** The main class for the Git Parcel mod. */
public class GitParcelMod {

  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  /** The display name for Git Parcel. */
  public static final String MOD_NAME = "Git Parcel";

  public static final String PROTOCOL_VERSION = "1";

  /** The logger instance for Git Parcel. */
  public static final Logger LOGGER = LogUtils.getLogger();

  public static Identifier identifier(String path) {
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }

  /**
   * Initializes the Git Parcel mod.
   *
   * <p>This method is called both on the client and server sides.
   */
  public static void init() {
    LOGGER.debug("Initializing");

    registerFormats();

    GameServerApi.ON_PLAYER_JOIN.on(
        e -> {
          var payload = UpdateParcelFormatInfosS2CPayload.from(ParcelFormatRegistry.INSTANCE);
          var serverPlayer = e.player();
          serverPlayer.connection.send(new ClientboundCustomPayloadPacket(payload));
        });

    registerGitParcelEvents();
  }

  private static void registerFormats() {
    ParcelFormatRegistry.INSTANCE.registerDefaultSaver(new ParcellaD32Saver());
    ParcelFormatRegistry.INSTANCE.register(new ParcellaD32Loader());
    ParcelFormatRegistry.INSTANCE.register(new ParcellaD16Loader());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelFormatRegistry.INSTANCE.register(new StructureTemplateFormat());
      ParcelFormatRegistry.INSTANCE.register(new ParcellaD16Saver());
      ParcelFormatRegistry.INSTANCE.register(new MvpFormat());
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
        registry,
        "gitparcel:parcel_instance",
        ParcelInstanceArgument.class,
        SingletonArgumentInfo.contextFree(ParcelInstanceArgument::instance));
  }

  public static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext context,
      Commands.CommandSelection commandSelection) {
    LOGGER.debug("Registering commands");

    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }

  private static void registerGitParcelEvents() {
    // Notify when player join
    GameServerApi.ON_PLAYER_JOIN.on(
        e -> {
          var player = e.player();
          var list = GitParcelLevelSavedData.get(player.level()).listParcelInstances();
          var payload = UpdateParcelInstancesS2CPayload.from(list);
          player.connection.send(new ClientboundCustomPayloadPacket(payload));
        });
    // Notify players when level parcel instances update
    GitParcelApi.Events.ON_UPDATE_PARCEL_INSTANCES.on(
        e ->
            e.level()
                .players()
                .forEach(
                    player -> {
                      var payload = UpdateParcelInstancesS2CPayload.from(e.list());
                      player.connection.send(new ClientboundCustomPayloadPacket(payload));
                    }));
  }
}
