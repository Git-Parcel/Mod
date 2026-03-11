package io.github.leawind.gitparcel.client;

import io.github.leawind.gitparcel.GitParcelMod;
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
        RegisterKeyMappingsEvent.class,
        event -> GitParcelOptions.registerKeyMappings(event::register));
  }
}
