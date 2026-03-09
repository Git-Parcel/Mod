package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public class FabricPlatformHelper implements IPlatformHelper {

  @Override
  public boolean isDevelopmentEnvironment() {

    return FabricLoader.getInstance().isDevelopmentEnvironment();
  }

  @Override
  public void register(KeyMapping keyMapping) {
    KeyBindingHelper.registerKeyBinding(keyMapping);
  }
}
