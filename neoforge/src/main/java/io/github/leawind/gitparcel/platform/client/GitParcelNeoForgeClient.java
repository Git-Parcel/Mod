package io.github.leawind.gitparcel.platform.client;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.client.GitParcelOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = GitParcel.MOD_ID, dist = Dist.CLIENT)
public final class GitParcelNeoForgeClient {
  public GitParcelNeoForgeClient(IEventBus eventBus) {
    GitParcelClient.init();
    GitParcelNeoForgeClient.init(eventBus);
  }

  public static void init(IEventBus eventBus) {}

  @EventBusSubscriber(modid = GitParcel.MOD_ID)
  public static class EventHandler {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      GitParcelOptions.registerKeyMappings(event::register);
    }
  }
}
