package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.restore;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RestoreSubcommand extends GitParcelBaseCommand {
  private RestoreSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("restore")
        .then(
            Commands.argument("revision", StringArgumentType.word())
                .executes(ctx -> restore(ctx, false))
                .then(
                    Commands.argument(
                            "ignore_entities",
                            BoolArgumentType.bool())
                        .executes(
                            ctx ->
                                restore(
                                    ctx,
                                    BoolArgumentType.getBool(
                                        ctx, "ignore_entities")))));
  }

  private static int restore(
      CommandContext<CommandSourceStack> ctx, boolean ignoreEntities)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.LOAD)) {
        return 0;
      }
    }

    String revision = StringArgumentType.getString(ctx, "revision");
    var service = ParcelService.get(source.getLevel());
    for (var parcel : parcels) {
      try {
        service.restoreParcel(parcel, revision, ignoreEntities);
        source.sendSystemMessage(
            Translations.of(
                "command.gitparcel.parcel.restore.success",
                parcel.uuid().toString(),
                revision));
      } catch (IOException | ParcelException e) {
        LOGGER.error(
            "Failed to restore parcel {} from revision {}",
            parcel.uuid(),
            revision,
            e);
        source.sendFailure(
            Translations.of(
                "command.gitparcel.parcel.restore.failure",
                parcel.uuid().toString(),
                describe(e)));
        return 0;
      } catch (Exception e) {
        LOGGER.error(
            "Unexpected error while restoring parcel {} from revision {}",
            parcel.uuid(),
            revision,
            e);
        source.sendFailure(
            Translations.of("command.gitparcel.parcel.unexpected_error", describe(e)));
        return 0;
      }
    }
    return 1;
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null ? "" : ": " + message);
  }
}
