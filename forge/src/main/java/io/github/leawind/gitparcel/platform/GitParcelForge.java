package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.platform.client.GitParcelForgeClient;
import io.github.leawind.gitparcel.platform.server.GitParcelForgeDedicatedServer;
import io.github.leawind.gitparcel.server.GitParcelDedicatedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GitParcel.MOD_ID)
public final class GitParcelForge {

  public GitParcelForge() {
    GitParcel.init();
    GitParcelForge.init();

    switch (FMLEnvironment.dist) {
      case Dist.CLIENT -> {
        GitParcelClient.init();
        GitParcelForgeClient.init();
      }
      case Dist.DEDICATED_SERVER -> {
        GitParcelDedicatedServer.init();
        GitParcelForgeDedicatedServer.init();
      }
    }
  }

  public static void init() {
    RegisterCommandsEvent.BUS.addListener(GitParcelForge::registerCommands);

    // TODO register custom payloads and receivers
  }

  public static void registerCommands(RegisterCommandsEvent event) {
    GitParcel.registerCommands(
        event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
  }
}
