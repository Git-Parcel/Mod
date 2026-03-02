package io.github.leawind.gitparcel;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import io.github.leawind.gitparcel.platform.Services;
import org.slf4j.Logger;

/** The main class for the Git Parcel mod. */
public class GitParcelMod {

  /** The mod ID for Git Parcel. */
  public static final String MOD_ID = "gitparcel";

  /** The display name for Git Parcel. */
  public static final String MOD_NAME = "Git Parcel";

  /** The logger instance for Git Parcel. */
  public static final Logger LOGGER = LogUtils.getLogger();

  /** The parcel format manager for handling different parcel formats. */
  public static final ParcelFormatManager PARCEL_FORMATS = new ParcelFormatManager();

  /**
   * Initializes the Git Parcel mod.
   *
   * <p>This method is called both on the client and server sides.
   */
  public static void init() {
    LOGGER.debug("Initializing");
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
