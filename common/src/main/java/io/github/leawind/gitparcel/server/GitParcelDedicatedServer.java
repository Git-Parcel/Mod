package io.github.leawind.gitparcel.server;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class GitParcelDedicatedServer {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static void init() {
    LOGGER.debug("Initializing Git Parcel mod dedicated server");
  }
}
