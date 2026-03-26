package io.github.leawind.gitparcel.server.commands.parcel_debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelWorldSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;

public class ClearDataSubcommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var dimension =
        Commands.argument("dimension", DimensionArgument.dimension())
            .executes(ClearDataSubcommand::clearDimension);

    var level =
        Commands.literal("level").executes(ClearDataSubcommand::clearCurrentLevel).then(dimension);

    var world = Commands.literal("world").executes(ClearDataSubcommand::clearWorld);

    return Commands.literal("clear_data").then(world).then(level);
  }

  private static int clearWorld(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    try {
      var server = source.getServer();
      GitParcelWorldSavedData.get(server).reset();

      sendSuccess(source, "command.gitparcel.parcel_debug.clear_data.world.success");
      return 1;
    } catch (Exception e) {
      ParcelDebugCommand.LOGGER.error("Unexpected error while clearing world data", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.unexpected_error", e.getMessage()));
      return 0;
    }
  }

  private static int clearCurrentLevel(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    return clearLevel(source, source.getLevel());
  }

  private static int clearDimension(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    var dimension = DimensionArgument.getDimension(ctx, "dimension");
    return clearLevel(ctx.getSource(), dimension);
  }

  private static int clearLevel(CommandSourceStack source, ServerLevel serverLevel) {
    try {
      GitParcelLevelSavedData.get(serverLevel).reset();

      sendSuccess(
          source,
          "command.gitparcel.parcel_debug.clear_data.level.success",
          serverLevel.dimension().identifier().toString());
      return 1;
    } catch (Exception e) {
      ParcelDebugCommand.LOGGER.error("Unexpected error while clearing level data", e);
      source.sendFailure(
          GitParcelTranslations.of(
              "command.gitparcel.parcel_debug.unexpected_error", e.getMessage()));
      return 0;
    }
  }

  private static void sendSuccess(CommandSourceStack source, String key, Object... args) {
    source.sendSuccess(() -> GitParcelTranslations.of(key, args), true);
  }
}
