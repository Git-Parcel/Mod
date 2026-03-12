package io.github.leawind.gitparcel.server.commands.parcel.instance.create;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ParcelInstanceSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("instance").then(ParcelInstanceNewSubcommand.build());
  }
}
