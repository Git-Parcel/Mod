package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.restore;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.SharedRepositoryArguments;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Explicitly restores a parcel snapshot, optionally saving the live world first. */
public final class RestoreSubcommand extends GitParcelBaseCommand {
  private RestoreSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("restore")
        .then(
            Commands.literal("recover")
                .then(
                    Commands.argument("operation_id", StringArgumentType.word())
                        .then(
                            Commands.literal("retry")
                                .executes(ctx -> recover(ctx, false)))
                        .then(
                            Commands.literal("rollback")
                                .executes(ctx -> recover(ctx, true)))))
        .then(
            Commands.argument("snapshot_id", StringArgumentType.word())
                .executes(ctx -> restore(ctx, RestoreSnapshotRequest.Mode.DIRECT))
                .then(
                    Commands.literal("save-first")
                        .executes(
                            ctx ->
                                restore(
                                    ctx,
                                    RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE))));
  }

  private static int restore(
      CommandContext<CommandSourceStack> ctx, RestoreSnapshotRequest.Mode mode)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = SharedRepositoryArguments.singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.RESTORE)
        || (mode == RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE
            && !validateParcelPermission(source, parcel, ParcelPermissions.SAVE))) {
      return 0;
    }

    final SnapshotId snapshot;
    try {
      snapshot = new SnapshotId(StringArgumentType.getString(ctx, "snapshot_id"));
    } catch (IllegalArgumentException e) {
      source.sendFailure(Translations.of("command.gitparcel.parcel.restore.failure", e.getMessage()));
      return 0;
    }

    var service = ParcelService.get(source.getLevel());
    var manager = OperationManager.get(source.getServer());
    var identity = snapshotIdentity(source);
    var operation =
        manager.submit(
            "restore_snapshot",
            parcel.uuid().toString(),
            operationOwner(source),
            progress -> {
              service.restoreSnapshotInBackground(
                  parcel, snapshot, mode, false, identity, progress, manager);
              return snapshot.value();
            },
            completed -> {
              if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
                source.sendSystemMessage(
                    Translations.of(
                        "command.gitparcel.parcel.restore.success",
                        parcel.uuid().toString(),
                        snapshot.abbreviate()));
              } else {
                source.sendFailure(
                    Translations.of(
                        "command.gitparcel.parcel.restore.failure",
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

  private static int recover(CommandContext<CommandSourceStack> ctx, boolean rollback)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcel = SharedRepositoryArguments.singleParcel(ctx);
    if (!validateParcelPermission(source, parcel, ParcelPermissions.RESTORE)) {
      return 0;
    }

    final UUID operationId;
    try {
      operationId = UUID.fromString(StringArgumentType.getString(ctx, "operation_id"));
    } catch (IllegalArgumentException e) {
      source.sendFailure(Translations.of("command.gitparcel.parcel.restore.failure", e.getMessage()));
      return 0;
    }

    var service = ParcelService.get(source.getLevel());
    var manager = OperationManager.get(source.getServer());
    var operation =
        manager.submit(
            rollback ? "rollback_restore" : "retry_restore",
            parcel.uuid().toString(),
            operationOwner(source),
            progress ->
                service
                    .resolvePendingRestoreInBackground(
                        parcel, operationId, rollback, false, progress, manager)
                    .restored()
                    .value(),
            completed -> {
              if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
                source.sendSystemMessage(
                    Translations.of(
                        "command.gitparcel.parcel.restore.recovery_success",
                        operationId,
                        parcel.uuid().toString(),
                        new SnapshotId(completed.result().orElseThrow()).abbreviate()));
              } else {
                source.sendFailure(
                    Translations.of(
                        "command.gitparcel.parcel.restore.failure",
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
