package io.github.leawind.gitparcel.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public class GitParcelModForgeClient {
  public static void init() {
    RegisterKeyMappingsEvent.BUS.addListener(
        event -> GitParcelOptions.registerKeyMappings(event::register));
  }
}
