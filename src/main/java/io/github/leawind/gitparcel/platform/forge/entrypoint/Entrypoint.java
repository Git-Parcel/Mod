/*? if forge {*/
/*package io.github.leawind.gitparcel.platform.forge.entrypoint;

import io.github.leawind.gitparcel.core.GitParcel;
import io.github.leawind.gitparcel.entrypoint.ModClientEntrypoint;
import io.github.leawind.gitparcel.entrypoint.ModEntrypoint;
import io.github.leawind.gitparcel.entrypoint.ModServerEntrypoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GitParcel.MOD_ID)
public class Entrypoint {
  public Entrypoint(final FMLJavaModLoadingContext context) {
    ModEntrypoint.initialize();
    Entrypoint.initialize();

    switch (FMLEnvironment.dist) {
      case CLIENT -> {
        ModClientEntrypoint.initialize();
        ClientEntrypoint.initialize();
      }
      case DEDICATED_SERVER -> {
        ModServerEntrypoint.initialize();
        ServerEntrypoint.initialize();
      }
    }
  }

  public static void initialize() {}
}
*//*?}*/
