package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.ParcelFormatManager;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16FormatV0;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32FormatV0;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormatV0;
import io.github.leawind.gitparcel.platform.Services;
import io.github.leawind.gitparcel.server.commands.ParcelCommand;
import io.github.leawind.gitparcel.server.commands.ParcelDebugCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

  public static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      Commands.CommandSelection environment,
      CommandBuildContext context) {

    LOGGER.debug("Registering commands");

    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }

  private static void registerFormats() {

    PARCEL_FORMATS.registerDefaultSaver(new ParcellaD32FormatV0.Save());
    PARCEL_FORMATS.register(new StructureTemplateFormatV0());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      PARCEL_FORMATS.register(new ParcellaD16FormatV0.Save());
      PARCEL_FORMATS.register(new MvpFormatV0());
    }
  }
}
