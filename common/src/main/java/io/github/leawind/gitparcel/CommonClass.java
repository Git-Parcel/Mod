package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.api.GameEvents;
import io.github.leawind.gitparcel.mixin.InvokeArgumentTypeInfos;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.commands.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.ParcelDebugCommand;
import io.github.leawind.gitparcel.server.commands.arguments.FilePathArgument;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;

public class CommonClass {

  public static void init() {
    GameEvents.REGISTER_COMMANDS =
        (event) -> {
          ParcelCommand.register(event.dispatcher(), event.context());

          if (Services.PLATFORM.isDevelopmentEnvironment()) {
            ParcelDebugCommand.register(event.dispatcher(), event.context());
          }
        };

    GameEvents.REGISTER_COMMAND_ARGUMENT_TYPES =
        (registry) -> {
          InvokeArgumentTypeInfos.register(
              registry,
              "file_path",
              FilePathArgument.class,
              SingletonArgumentInfo.contextFree(FilePathArgument::filePath));
        };
  }
}
