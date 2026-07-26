package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.common.api.permission.PermissionLevel;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.create.CreateSubcommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.formats.FormatsSubcommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.repositories.OperationsSubcommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.repositories.RepositoriesSubcommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelsCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {

    final var parcels =
        Commands.literal("parcels")
            .requires(MinecraftPermissions.require(PermissionLevel.ALL))
            .then(CreateSubcommand.build())
            .then(FormatsSubcommand.build())
            .then(OperationsSubcommand.build())
            .then(RepositoriesSubcommand.build());

    dispatcher.register(parcels);
  }
}
