package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

  @Override
  public boolean isDevelopmentEnvironment() {

    return !FMLLoader.getCurrent().isProduction();
  }
}
