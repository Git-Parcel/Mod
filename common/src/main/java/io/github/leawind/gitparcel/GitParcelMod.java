package io.github.leawind.gitparcel;

import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import io.github.leawind.gitparcel.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitParcelMod {

  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  /** The display name for Git Parcel. */
  public static final String MOD_NAME = "Git Parcel";

  /** The logger instance for Git Parcel. */
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

  /** The parcel format manager for handling different parcel formats. */
  public static final ParcelFormatManager PARCEL_FORMATS = new ParcelFormatManager();

  public static void init() {
    registerFormats();
  }

  private static void registerFormats() {
    var formatManager = PARCEL_FORMATS;

    formatManager.registerDefaultSaver(new ParcellaD32FormatV0.Save());
    formatManager.register(new StructureTemplateFormatV0());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      formatManager.register(new ParcellaD16FormatV0.Save());
      formatManager.register(new MvpFormatV0());
    }
  }
}
