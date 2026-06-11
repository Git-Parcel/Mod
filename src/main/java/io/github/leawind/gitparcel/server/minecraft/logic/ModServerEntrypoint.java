package io.github.leawind.gitparcel.server.minecraft.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModServerEntrypoint {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModServerEntrypoint.class);

  public static void initialize() {
    LOGGER.debug("Initializing Git Parcel mod dedicated server");
  }
}
