package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.publish;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.SharedRepositoryArguments;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ServerQueryHandler;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class PublishSubcommand extends GitParcelBaseCommand {
  private static final String ARG_REPOSITORY = "repository";
  private static final String ARG_PATH = "path";

  private PublishSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("publish")
        .then(
            Commands.argument(ARG_REPOSITORY, StringArgumentType.word())
                .suggests(SharedRepositoryArguments::suggestRepositories)
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
    if (!validateWorldPermission(source, WorldPermissions.PUBLISH_IMPORT)) {
      return 0;
    }
    var parcel = SharedRepositoryArguments.singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.VIEW)) {
      return 0;
    }

    String repository = StringArgumentType.getString(ctx, ARG_REPOSITORY);
    String path = StringArgumentType.getString(ctx, ARG_PATH);
    String message =
        requestedMessage == null
            ? "Publish parcel snapshot"
            : requestedMessage;
    var service = ParcelService.get(source.getLevel());
    var manager = OperationManager.get(source.getServer());
    var identity = snapshotIdentity(source);
    var operation =
        manager.submit(
            "publish_snapshot",
            repository + ":" + path,
            operationOwner(source),
            progress ->
                service
                    .publishCurrentSnapshotInBackground(
                        parcel,
                        repository,
                        path,
                        message,
                        identity,
                        progress,
                        manager)
                    .revision(),
            completed -> {
              if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
                source.sendSystemMessage(
                    Translations.of(
                        "command.gitparcel.parcel.publish.success",
                        parcel.uuid().toString(),
                        repository,
                        path,
                        abbreviate(completed.result().orElseThrow())));
                if (source.getEntity() instanceof ServerPlayer player) {
                  ServerQueryHandler.syncRepositories(player);
                }
              } else {
                source.sendFailure(
                    Translations.of(
                        "command.gitparcel.parcel.publish.failure",
                        completed.error().orElse("Failed")));
              }
            });
    if (operation.state() == OperationSnapshot.State.FAILED) {
      return 0;
    }
    source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.git_operation.started",
              operation.operationId(),
              operation.kind(),
              operation.target()));
    return 1;
  }
}
