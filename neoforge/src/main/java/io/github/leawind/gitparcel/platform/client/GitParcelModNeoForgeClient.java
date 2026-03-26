package io.github.leawind.gitparcel.platform.client;

import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.client.GitParcelOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = GitParcelMod.MOD_ID, dist = Dist.CLIENT)
public final class GitParcelModNeoForgeClient {
  public GitParcelModNeoForgeClient(IEventBus eventBus) {
    GitParcelModClient.init();
    GitParcelModNeoForgeClient.init(eventBus);
  }

  public static void init(IEventBus eventBus) {}

  @EventBusSubscriber(modid = GitParcelMod.MOD_ID)
  public static class EventHandler {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      GitParcelOptions.registerKeyMappings(event::register);
    }
  }
}
