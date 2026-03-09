package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.client.GitParcelModNeoForgeClient;
import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

  @Override
  public boolean isDevelopmentEnvironment() {

    return !FMLLoader.getCurrent().isProduction();
  }

  @Override
  public void register(KeyMapping keyMapping) {
    GitParcelModNeoForgeClient.KEY_MAPPINGS.add(keyMapping);
  }
}
