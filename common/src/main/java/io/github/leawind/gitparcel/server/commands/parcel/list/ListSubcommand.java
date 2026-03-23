package io.github.leawind.gitparcel.server.commands.parcel.list;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class ListSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {

    var dimension =
        Commands.argument("dimension", DimensionArgument.dimension())
            .executes(ListSubcommand::list2);

    return Commands.literal("list").executes(ListSubcommand::list1).then(dimension);
  }

  private static int list1(CommandContext<CommandSourceStack> ctx) {
    var serverLevel = ctx.getSource().getLevel();
    return list(ctx.getSource(), serverLevel);
  }

  private static int list2(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    var serverLevel = DimensionArgument.getDimension(ctx, "dimension");
    return list(ctx.getSource(), serverLevel);
  }

  private static int list(CommandSourceStack source, ServerLevel serverLevel) {
    if (!validateWorldPermission(source, WorldPermissions.LIST_PARCELS)) {
      return 0;
    }

    var savedData = GitParcelLevelSavedData.get(serverLevel);
    List<Parcel> parcels = savedData.listParcels();

    source.sendSuccess(
        () -> GitParcelTranslations.of("command.gitparcel.parcel.list.header", parcels.size()),
        false);

    for (var parcel : parcels) {
      source.sendSuccess(
          () ->
              Component.literal("  - UUID: ")
                  .append(Component.literal(parcel.uuid().toString()))
                  .append(Component.literal(", Box: "))
                  .append(Component.literal(parcel.boundingBox().toString()))
                  .append(Component.literal(", Rotation: "))
                  .append(Component.literal(parcel.rotation().name()))
                  .append(Component.literal(", Mirror: "))
                  .append(Component.literal(parcel.mirror().name())),
          false);
    }

    return 1;
  }
}
