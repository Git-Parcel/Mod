package io.github.leawind.gitparcel.common.platform.forge;

/*? if forge {*/
/*
import io.github.leawind.gitparcel.api.GitParcel;
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.server.minecraft.logic.ModServerEntrypoint;
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
