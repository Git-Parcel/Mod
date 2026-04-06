package io.github.leawind.gitparcel.platform.client;

import io.github.leawind.gitparcel.client.GitParcelOptions;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public final class GitParcelForgeClient {
  public static void init() {
    RegisterKeyMappingsEvent.BUS.addListener(
        event -> GitParcelOptions.registerKeyMappings(event::register));
  }
}
