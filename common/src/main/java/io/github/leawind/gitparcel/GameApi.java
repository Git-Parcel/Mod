package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.mixin.InvokeArgumentTypeInfos;
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

/**
 * Some methods to register things to Minecraft.
 *
 * <p>These methods are expected to be called by mixins.
 */
public final class GameApi {
  private static final Logger LOGGER = LogUtils.getLogger();

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

}
