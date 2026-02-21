package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

public class CommonClass {

  public static void init() {
    Constants.LOG.info(
        "Environment: {}",
        Services.PLATFORM.isDevelopmentEnvironment() ? "development" : "production");
  }
}
