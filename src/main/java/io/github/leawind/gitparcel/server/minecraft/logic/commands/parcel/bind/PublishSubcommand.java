package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.bind;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PublishSubcommand extends GitParcelBaseCommand {
  private static final String ARG_REPOSITORY = "repository";
  private static final String ARG_PATH = "path";

  private PublishSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("publish")
        .then(
            Commands.argument(ARG_REPOSITORY, StringArgumentType.word())
                .suggests(BindSubcommand::suggestRepositories)
                .then(
                    Commands.argument(ARG_PATH, StringArgumentType.string())
                        .executes(ctx -> publish(ctx, null))
                        .then(
                            Commands.argument("message", StringArgumentType.greedyString())
                                .executes(
                                    ctx ->
                                        publish(
                                            ctx,
                                            StringArgumentType.getString(
                                                ctx, "message"))))));
  }

  private static int publish(
      CommandContext<CommandSourceStack> ctx, String requestedMessage)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      return 0;
    }
    var parcel = BindSubcommand.singleParcel(ctx);
    for (var permission :
        java.util.List.of(
            ParcelPermissions.SAVE,
            ParcelPermissions.CONFIG,
            ParcelPermissions.COMMIT)) {
      if (!validateParcelPermission(source, parcel, permission)) {
        return 0;
      }
    }

    String repository = StringArgumentType.getString(ctx, ARG_REPOSITORY);
    String path = StringArgumentType.getString(ctx, ARG_PATH);
    String message =
        requestedMessage == null
            ? "Publish parcel " + parcel.uuid()
            : requestedMessage;
    try {
      var result =
          ParcelService.get(source.getLevel())
              .publishParcel(
                  parcel,
                  repository,
                  path,
                  message,
                  identity(source),
                  false)
              .orElseThrow();
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.publish.success",
              parcel.uuid().toString(),
              repository,
              path,
              abbreviate(result.revision())));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Failed to publish parcel {} to {}/{}", parcel.uuid(), repository, path, e);
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.publish.failure", BindSubcommand.describe(e)));
      return 0;
    }
  }

  private static GitRepo.CommitIdentity identity(CommandSourceStack source) {
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
}
