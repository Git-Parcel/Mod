package io.github.leawind.gitparcel.common.platform.forge;

/*? if forge {*/
/*
import io.github.leawind.gitparcel.client.minecraft.logic.ModClientEntrypoint;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.gitparcel.common.minecraft.logic.ModEntrypoint;
import io.github.leawind.gitparcel.server.minecraft.logic.ModServerEntrypoint;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(GitParcel.MOD_ID)
public class Entrypoint {
  public Entrypoint(FMLJavaModLoadingContext context) {
    ModEntrypoint.initialize();

    var argumentTypes = new CommandArgumentTypeRegistrarImpl();
    ModEntrypoint.registerCommandArgumentTypes(argumentTypes);
    argumentTypes.attach(context.getModEventBus());

    ServerNetworkingImpl.registerPayload();

    MinecraftForge.EVENT_BUS.addListener(Entrypoint::onRegisterCommands);
    MinecraftForge.EVENT_BUS.addListener(Entrypoint::onPlayerLoggedIn);
    MinecraftForge.EVENT_BUS.addListener(Entrypoint::onPlayerChangedDimension);
    MinecraftForge.EVENT_BUS.addListener(Entrypoint::onServerStarted);
    MinecraftForge.EVENT_BUS.addListener(Entrypoint::onServerStopping);

    if (FMLEnvironment.dist == Dist.CLIENT) {
      ModClientEntrypoint.initialize();
    } else if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
      ModServerEntrypoint.initialize();
    }
  }

  private static void onRegisterCommands(RegisterCommandsEvent event) {
    ModEntrypoint.registerCommands(
        event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
  }

  private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ModEntrypoint.onPlayerJoin(player);
    }
  }

  private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      ModEntrypoint.onPlayerChangeDimension(player);
    }
  }

  private static void onServerStarted(ServerStartedEvent event) {
    ModEntrypoint.onServerStarted(event.getServer());
  }

  private static void onServerStopping(ServerStoppingEvent event) {
    ModEntrypoint.onServerStopping(event.getServer());
  }
}
*//*?}*/
