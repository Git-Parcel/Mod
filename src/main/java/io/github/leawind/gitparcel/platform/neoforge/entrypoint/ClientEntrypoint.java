/*? if neoforge {*/
/*package io.github.leawind.gitparcel.platform.neoforge.entrypoint;

import io.github.leawind.gitparcel.core.api.GitParcel;
import io.github.leawind.gitparcel.entrypoint.ModClientEntrypoint;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = GitParcel.MOD_ID, dist = Dist.CLIENT)
public class ClientEntrypoint {
  public ClientEntrypoint(IEventBus eventBus) {
    ModClientEntrypoint.initialize();
    ClientEntrypoint.initialize(eventBus);
  }

  public static void initialize(IEventBus eventBus) {}

  @EventBusSubscriber(modid = GitParcel.MOD_ID)
  public static class EventHandler {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
      // register key mappings
    }
  }
}
*//*?}*/
