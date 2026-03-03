package io.github.leawind.gitparcel;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = GitParcelMod.MOD_ID, dist = Dist.CLIENT)
public class GitParcelModNeoForgeClient {
  public GitParcelModNeoForgeClient(IEventBus eventBus) {
    GitParcelModClient.init();
  }
}
