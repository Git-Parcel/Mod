package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.api.GameEvents;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.commands.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.ParcelDebugCommand;

public class CommonClass {

  public static void init() {
    GameEvents.REGISTER_COMMANDS =
        (event) -> {
          ParcelCommand.register(event.dispatcher(), event.context());

          if (Services.PLATFORM.isDevelopmentEnvironment()) {
            ParcelDebugCommand.register(event.dispatcher(), event.context());
          }
        };
  }
}
