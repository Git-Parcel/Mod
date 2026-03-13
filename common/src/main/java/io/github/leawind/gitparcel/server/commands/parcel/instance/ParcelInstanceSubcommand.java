package io.github.leawind.gitparcel.server.commands.parcel.instance;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.server.commands.parcel.instance.create.ParcelInstanceCreateSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.instance.delete.ParcelInstanceDeleteSubcommand;
import io.github.leawind.gitparcel.server.commands.parcel.instance.list.ParcelInstanceListSubcommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelInstanceSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("instance")
        .then(ParcelInstanceCreateSubcommand.build())
        .then(ParcelInstanceDeleteSubcommand.build())
        .then(ParcelInstanceListSubcommand.build());
  }
}
