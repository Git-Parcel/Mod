package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.save;

import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Atomic world-capture and snapshot-commit command. */
public final class SaveSubcommand extends GitParcelBaseCommand {
  private SaveSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("save")
        .executes(ctx -> save(ctx, null))
        .then(
            Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> save(ctx, StringArgumentType.getString(ctx, "name"))));
  }

  private static int save(CommandContext<CommandSourceStack> ctx, String requestedName)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.SAVE)) {
        return 0;
      }
    }

    var service = SnapshotService.get(source.getLevel());
    var manager = OperationManager.get(source.getServer());
    int accepted = 0;
    for (var parcel : parcels) {
      String name =
          requestedName == null || requestedName.isBlank()
              ? "Snapshot"
              : requestedName;
      var identity = snapshotIdentity(source);
      var operation =
          manager.submit(
              "save_snapshot",
              parcel.uuid().toString(),
              operationOwner(source),
              progress ->
                  service
                      .saveSnapshotInBackground(
                          parcel,
                          name,
                          "",
                          identity,
                          false,
                          progress,
                          manager)
                      .value(),
              completed -> {
                if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
                  String snapshot = completed.result().orElseThrow();
                  source.sendSystemMessage(
                      Translations.of(
                          "command.gitparcel.parcel.save.success",
                          parcel.uuid().toString(),
                          abbreviate(snapshot)));
                } else {
                  source.sendFailure(
                      Translations.of(
                          "command.gitparcel.parcel.save.failure",
                          completed.error().orElse("Failed")));
                }
              });
      if (operation.state() != OperationSnapshot.State.FAILED) {
        accepted++;
        source.sendSystemMessage(
            Translations.of(
                "command.gitparcel.git_operation.started",
                operation.operationId(),
                operation.kind(),
                operation.target()));
      }
    }
    return accepted;
  }
}
