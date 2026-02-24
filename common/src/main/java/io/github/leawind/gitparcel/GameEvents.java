package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.mixin.InvokeArgumentTypeInfos;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.commands.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.ParcelDebugCommand;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import io.github.leawind.gitparcel.server.commands.arguments.ParcelFormatArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;

public final class GameEvents {
  public static void registerCommandArgumentTypes(Registry<ArgumentTypeInfo<?, ?>> registry) {
    InvokeArgumentTypeInfos.register(
        registry,
        "gitparcel:file_path",
        FilePathArgument.class,
        SingletonArgumentInfo.contextFree(FilePathArgument::filePath));

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
      Commands.CommandSelection selection,
      CommandBuildContext context) {
    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }
}
