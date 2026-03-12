package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;

public class ParcelDebugCommand {
  static final Logger LOGGER = LogUtils.getLogger();

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    var parcel_debug =
        Commands.literal("parcel_debug")
            .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
            .then(ParcelDebugSaveSubcommand.build())
            .then(ParcelDebugLoadSubcommand.build());

    dispatcher.register(parcel_debug);
  }
}
