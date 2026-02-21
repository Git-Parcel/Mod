package io.github.leawind.gitparcel.api;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.function.Consumer;

public final class GameEvents {
  public static Consumer<RegiterCommands> REGISTER_COMMANDS = null;

  public record RegiterCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      Commands.CommandSelection selection,
      CommandBuildContext context) {}
}
