package io.github.leawind.gitparcel.platform;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.platform.services.IPlatformHelper;
import java.util.ServiceLoader;
import org.slf4j.Logger;

public final class Services {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

  public static <T> T load(Class<T> clazz) {
    var loadedService =
        ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(
                () -> new NullPointerException("Failed to load service for " + clazz.getName()));

    LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
    return loadedService;
  }
}
