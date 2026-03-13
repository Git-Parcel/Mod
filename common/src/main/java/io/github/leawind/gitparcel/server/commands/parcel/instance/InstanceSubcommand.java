package io.github.leawind.gitparcel.server.commands.parcel.instance;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.server.commands.parcel.instance.create.CreateSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.instance.delete.DeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.instance.list.ListSubcommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class InstanceSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("instance")
        .then(CreateSubcommand.build())
        .then(DeleteSubcommand.build())
        .then(ListSubcommand.build());
  }
}
