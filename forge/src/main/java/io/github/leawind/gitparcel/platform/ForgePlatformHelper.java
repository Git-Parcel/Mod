package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.client.GitParcelModForgeClient;
import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatformHelper implements IPlatformHelper {

  @Override
  public boolean isDevelopmentEnvironment() {

    return !FMLLoader.isProduction();
  }

  @Override
  public void register(KeyMapping keyMapping) {
    GitParcelModForgeClient.KEY_MAPPINGS.add(keyMapping);
  }
}
