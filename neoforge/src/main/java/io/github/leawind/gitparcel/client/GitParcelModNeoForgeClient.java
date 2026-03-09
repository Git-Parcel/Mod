package io.github.leawind.gitparcel.client;

import io.github.leawind.gitparcel.GitParcelMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = GitParcelMod.MOD_ID, dist = Dist.CLIENT)
public class GitParcelModNeoForgeClient {
  public GitParcelModNeoForgeClient(IEventBus eventBus) {
    GitParcelModClient.init();
    GitParcelModNeoForgeClient.init(eventBus);
  }

  public static void init(IEventBus eventBus) {

    eventBus.addListener(
        RegisterKeyMappingsEvent.class, event -> KEY_MAPPINGS.forEach(event::register));
  }

  /**
   * This list is updated in mod client init, and then registered in platform init.
   *
   * @see io.github.leawind.gitparcel.platform.NeoForgePlatformHelper#register(KeyMapping)
   * @see #init(IEventBus)
   */
  public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
}
