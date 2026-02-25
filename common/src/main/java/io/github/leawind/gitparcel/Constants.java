package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
  public static final String MOD_ID = "gitparcel";
  public static final String MOD_NAME = "Git Parcel";
  public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

  public static final ParcelFormatManager PARCEL_FORMATS = new ParcelFormatManager();
}
