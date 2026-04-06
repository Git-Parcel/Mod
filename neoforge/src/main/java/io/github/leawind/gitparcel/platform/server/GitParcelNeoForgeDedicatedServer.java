package io.github.leawind.gitparcel.platform.server;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.server.GitParcelDedicatedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = GitParcel.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class GitParcelNeoForgeDedicatedServer {
  public GitParcelNeoForgeDedicatedServer(IEventBus modBus) {
    GitParcelDedicatedServer.init();
    GitParcelNeoForgeDedicatedServer.init();
  }

  public static void init() {}
}
