package io.github.leawind.gitparcel.core;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** The main class for the Git Parcel mod. */
public final class GitParcel {

  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  public static final String PROTOCOL_VERSION = "1";

  /** The logger instance for Git Parcel. */
  public static final Logger LOGGER = LogUtils.getLogger();

  public static Identifier identifier(String path) {
    return Identifier.fromNamespaceAndPath(MOD_ID, path);
  }
}
