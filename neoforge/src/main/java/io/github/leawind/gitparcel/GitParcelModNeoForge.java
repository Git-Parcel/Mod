package io.github.leawind.gitparcel;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(GitParcelMod.MOD_ID)
public class GitParcelModNeoForge {

  public GitParcelModNeoForge(IEventBus eventBus) {
    GitParcelMod.init();
  }

  @EventBusSubscriber(modid = GitParcelMod.MOD_ID)
  public static class EventHandler {
    @SubscribeEvent
    public static void onLivingJump(RegisterCommandsEvent event) {
      GitParcelMod.registerCommands(
          event.getDispatcher(), event.getCommandSelection(), event.getBuildContext());
    }
  }
}
