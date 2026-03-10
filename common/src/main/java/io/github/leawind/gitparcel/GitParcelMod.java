package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.GitParcelApi;
import io.github.leawind.gitparcel.mixin.InvokeArgumentTypeInfos;
import io.github.leawind.gitparcel.parcelformats.mvp.MvpFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Loader;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.parcelformats.structuretemplate.StructureTemplateFormat;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.commands.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.ParcelDebugCommand;
import io.github.leawind.gitparcel.server.commands.arguments.DirPathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

/** The main class for the Git Parcel mod. */
public class GitParcelMod {

  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  /** The display name for Git Parcel. */
  public static final String MOD_NAME = "Git Parcel";

  /** The logger instance for Git Parcel. */
  public static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Initializes the Git Parcel mod.
   *
   * <p>This method is called both on the client and server sides.
   */
  public static void init() {
    LOGGER.debug("Initializing");

    registerFormats();
  }

  private static void registerFormats() {

    GitParcelApi.FORMAT_REGISTRY.registerDefaultSaver(new ParcellaD32Saver());
    GitParcelApi.FORMAT_REGISTRY.register(new ParcellaD32Loader());
    GitParcelApi.FORMAT_REGISTRY.register(new ParcellaD16Loader());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      GitParcelApi.FORMAT_REGISTRY.register(new StructureTemplateFormat());
      GitParcelApi.FORMAT_REGISTRY.register(new ParcellaD16Saver());
      GitParcelApi.FORMAT_REGISTRY.register(new MvpFormat());
    }
  }

  public static void registerCommandArgumentTypes(Registry<ArgumentTypeInfo<?, ?>> registry) {
    LOGGER.debug("Registering command argument types");

    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:file_path",
        FilePathArgument.class,
        SingletonArgumentInfo.contextFree(FilePathArgument::path));

    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:dir_path",
        DirPathArgument.class,
        SingletonArgumentInfo.contextFree(DirPathArgument::path));

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
  }

  public static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      Commands.CommandSelection commandSelection,
      CommandBuildContext context) {
    LOGGER.debug("Registering commands");

    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }
}
