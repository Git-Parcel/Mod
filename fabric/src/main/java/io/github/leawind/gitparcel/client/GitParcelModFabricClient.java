package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.slf4j.Logger;

public class GitParcelModFabricClient implements ClientModInitializer {
  public static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void onInitializeClient() {
    GitParcelModClient.init();
    GitParcelModFabricClient.init();
  }

  public static void init() {
    // Register key mappings
    LOGGER.debug("Registering key mappings");
    GitParcelOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
  }
}
