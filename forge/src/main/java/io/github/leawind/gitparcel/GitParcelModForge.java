package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.client.GitParcelModForgeClient;
import io.github.leawind.gitparcel.server.GitParcelModDedicatedServer;
import io.github.leawind.gitparcel.server.GitParcelModForgeDedicatedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GitParcelMod.MOD_ID)
public class GitParcelModForge {

  public GitParcelModForge() {
    GitParcelMod.init();
    GitParcelModForge.init();

    switch (FMLEnvironment.dist) {
      case Dist.CLIENT -> {
        GitParcelModClient.init();
        GitParcelModForgeClient.init();
      }
      case Dist.DEDICATED_SERVER -> {
        GitParcelModDedicatedServer.init();
        GitParcelModForgeDedicatedServer.init();
      }
    }
  }

  public static void init() {
    RegisterCommandsEvent.BUS.addListener(GitParcelModForge::registerCommands);

    // TODO register custom payloads and receivers
  }

  public static void registerCommands(RegisterCommandsEvent event) {
    GitParcelMod.registerCommands(
        event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
  }
}
