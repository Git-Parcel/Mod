package io.github.leawind.gitparcel.server;

import io.github.leawind.gitparcel.GitParcelMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = GitParcelMod.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class GitParcelModNeoForgeDedicatedServer {
  public GitParcelModNeoForgeDedicatedServer(IEventBus modBus) {
    GitParcelModDedicatedServer.init();
    GitParcelModNeoForgeDedicatedServer.init();
  }

  public static void init() {}
}
