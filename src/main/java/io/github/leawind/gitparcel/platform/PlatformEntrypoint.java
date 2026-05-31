package io.github.leawind.gitparcel.platform;

/*? if fabric {*/
import net.fabricmc.api.ModInitializer;

public class PlatformEntrypoint implements ModInitializer {
  @Override
  public void onInitialize() {}
}

/*?}*/

/*? if forge {*/
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("gitparcel")
public class PlatformEntrypoint {
  public PlatformEntrypoint(final FMLJavaModLoadingContext context) {}
}

*//*?}*/

/*? if neoforge {*/
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("gitparcel")
public class PlatformEntrypoint {
  public PlatformEntrypoint(IEventBus modEventBus, ModContainer modContainer) {}
}
*//*?}*/
