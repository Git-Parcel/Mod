package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.server.commands.parcel.config.ConfigSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.create.CreateSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.delete.DeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.formats.FormatsSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.list.ListSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.save.SaveSubcommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;

public class ParcelCommand {
  public static final Logger LOGGER = LogUtils.getLogger();

  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcel =
        Commands.literal("parcel")
            .requires(Commands.hasPermission(Commands.LEVEL_ALL))
            .then(ConfigSubcommand.build())
            .then(FormatsSubcommand.build())
            .then(CreateSubcommand.build())
            .then(DeleteSubcommand.build())
            .then(ListSubcommand.build())
            .then(SaveSubcommand.build());

    dispatcher.register(parcel);
  }
}
