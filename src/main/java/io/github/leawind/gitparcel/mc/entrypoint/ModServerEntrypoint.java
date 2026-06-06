package io.github.leawind.gitparcel.mc.entrypoint;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ModServerEntrypoint {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static void initialize() {
    LOGGER.debug("Initializing Git Parcel mod dedicated server");
  }
}
