package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.GitParcelApi;
import io.github.leawind.gitparcel.parcel.formats.mvp.MvpFormat;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD16Format;
import io.github.leawind.gitparcel.parcel.formats.parcella.ParcellaD32Format;
import io.github.leawind.gitparcel.parcel.formats.structuretemplate.StructureTemplateFormat;
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
      Commands.CommandSelection commandSelection,
      CommandBuildContext context) {

    LOGGER.debug("Registering commands");

    ParcelCommand.register(dispatcher, context);

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      ParcelDebugCommand.register(dispatcher, context);
    }
  }

  private static void registerFormats() {

    GitParcelApi.PARCEL_FORMATS.registerDefaultSaver(new ParcellaD32Format.Save());
    GitParcelApi.PARCEL_FORMATS.register(new StructureTemplateFormat());

    if (Services.PLATFORM.isDevelopmentEnvironment()) {
      GitParcelApi.PARCEL_FORMATS.register(new ParcellaD16Format.Save());
      GitParcelApi.PARCEL_FORMATS.register(new MvpFormat());
    }
  }
}
