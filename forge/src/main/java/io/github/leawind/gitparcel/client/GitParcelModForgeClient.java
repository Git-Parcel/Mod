package io.github.leawind.gitparcel.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public class GitParcelModForgeClient {
  public static void init() {
    RegisterKeyMappingsEvent.BUS.addListener(event -> KEY_MAPPINGS.forEach(event::register));
  }

  /**
   * This list is updated in mod client init, and then registered in platform init.
   *
   * @see io.github.leawind.gitparcel.platform.ForgePlatformHelper#register(KeyMapping)
   * @see #init()
   */
  public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
}
