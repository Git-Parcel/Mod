package io.github.leawind.gitparcel.platform.services;

import net.minecraft.client.KeyMapping;

public interface IPlatformHelper {

  boolean isDevelopmentEnvironment();

  void register(KeyMapping keyMapping);
}
