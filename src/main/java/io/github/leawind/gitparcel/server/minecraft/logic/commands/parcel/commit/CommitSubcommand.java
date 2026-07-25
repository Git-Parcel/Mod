package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.commit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CommitSubcommand extends GitParcelBaseCommand {
  private CommitSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("commit")
        .executes(ctx -> commit(ctx, null))
        .then(
            Commands.argument("message", StringArgumentType.greedyString())
                .executes(
                    ctx ->
                        commit(
                            ctx,
                            StringArgumentType.getString(ctx, "message"))));
  }

  private static int commit(
      CommandContext<CommandSourceStack> ctx, String requestedMessage)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.COMMIT)) {
        return 0;
      }
    }

    var service = ParcelService.get(source.getLevel());
    var identity = commitIdentity(source);
    for (var parcel : parcels) {
      String message =
          requestedMessage == null
              ? "Save parcel " + parcel.uuid()
              : requestedMessage;
      try {
        var result = service.commitParcel(parcel, message, identity);
        if (result.isPresent()) {
          source.sendSystemMessage(
              Translations.of(
                  "command.gitparcel.parcel.commit.success",
                  parcel.uuid().toString(),
                  abbreviate(result.orElseThrow().revision())));
        } else {
          source.sendSystemMessage(
              Translations.of(
                  "command.gitparcel.parcel.commit.no_changes",
                  parcel.uuid().toString()));
        }
      } catch (IOException | ParcelException e) {
        LOGGER.error("Failed to commit parcel {}", parcel.uuid(), e);
        source.sendFailure(
            Translations.of(
                "command.gitparcel.parcel.commit.failure",
                parcel.uuid().toString(),
                describe(e)));
        return 0;
      } catch (Exception e) {
        LOGGER.error("Unexpected error while committing parcel {}", parcel.uuid(), e);
        source.sendFailure(
            Translations.of("command.gitparcel.parcel.unexpected_error", describe(e)));
        return 0;
      }
    }
    return 1;
  }

  private static GitRepo.CommitIdentity commitIdentity(CommandSourceStack source) {
    String name =
        source
            .getTextName()
            .replace('<', '_')
            .replace('>', '_')
            .replace('\n', ' ')
            .replace('\r', ' ');
    if (name.isBlank()) {
      name = "Minecraft Server";
    }
    var entity = source.getEntity();
    String email =
        entity == null
            ? "server@gitparcel.local"
            : entity.getUUID() + "@gitparcel.local";
    return new GitRepo.CommitIdentity(name, email);
  }

  private static String abbreviate(String revision) {
    return revision.substring(0, Math.min(8, revision.length()));
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null ? "" : ": " + message);
  }
}
