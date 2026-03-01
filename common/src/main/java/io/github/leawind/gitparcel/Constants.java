package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains constants and shared resources for the Git Parcel mod. */
public class Constants {
  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  /** The display name for Git Parcel. */
  public static final String MOD_NAME = "Git Parcel";

  /** The logger instance for Git Parcel. */
  public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

  /** The parcel format manager for handling different parcel formats. */
  public static final ParcelFormatManager PARCEL_FORMATS = new ParcelFormatManager();
}
