package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.server.GitParcelModDedicatedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GitParcelMod.MOD_ID)
public class GitParcelModForge {

  public GitParcelModForge() {

    registerEvents();

    GitParcelMod.init();

    switch (FMLEnvironment.dist) {
      case Dist.CLIENT -> GitParcelModClient.init();
      case Dist.DEDICATED_SERVER -> GitParcelModDedicatedServer.init();
    }
  }

  private static void registerEvents() {

    RegisterCommandsEvent.BUS.addListener(
        (x) ->
            GitParcelMod.registerCommands(
                x.getDispatcher(), x.getCommandSelection(), x.getBuildContext()));
  }
}
