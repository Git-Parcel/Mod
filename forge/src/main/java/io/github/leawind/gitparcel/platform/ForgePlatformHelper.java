package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatformHelper implements IPlatformHelper {

  @Override
  public boolean isDevelopmentEnvironment() {

    return !FMLLoader.isProduction();
  }
}
