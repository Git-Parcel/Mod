package io.github.leawind.gitparcel.platform.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.client.GitParcelOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.slf4j.Logger;

public final class GitParcelFabricClient implements ClientModInitializer {
  public static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void onInitializeClient() {
    GitParcelClient.init();
    GitParcelFabricClient.init();
  }

  public static void init() {
    // Register key mappings
    LOGGER.debug("Registering key mappings");
    GitParcelOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
  }
}
