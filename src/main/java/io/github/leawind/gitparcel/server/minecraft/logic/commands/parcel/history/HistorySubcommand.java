package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.history;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Displays the logical snapshot tree, including parent and current-baseline information. */
public final class HistorySubcommand extends GitParcelBaseCommand {
  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;

  private HistorySubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("history")
        .executes(ctx -> history(ctx, DEFAULT_LIMIT))
        .then(
            Commands.argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                .executes(ctx -> history(ctx, IntegerArgumentType.getInteger(ctx, "limit"))));
  }

  private static int history(CommandContext<CommandSourceStack> ctx, int limit)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);
    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.VIEW)) {
        return 0;
      }
    }

    var service = ParcelService.get(source.getLevel());
    var manager = OperationManager.get(source.getServer());
    int accepted = 0;
    for (var parcel : parcels) {
      var result = new AtomicReference<SnapshotTreePage>();
      var operation =
          manager.submit(
              "query_snapshot_tree",
              parcel.uuid().toString(),
              operationOwner(source),
              ignored -> {
                var tree =
                    service.querySnapshotTreeInBackground(
                        parcel, limit, Optional.empty(), manager);
                result.set(tree);
                return tree.nodes().size() + " snapshot nodes";
              },
              completed -> {
                if (completed.state() == OperationSnapshot.State.SUCCEEDED) {
                  sendTree(source, parcel, result.get());
                } else {
                  source.sendFailure(
                      Translations.of(
                          "command.gitparcel.parcel.history.failure",
                          parcel.uuid().toString(),
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

  private static void sendTree(
      CommandSourceStack source, Parcel parcel, SnapshotTreePage tree) {
    if (tree.nodes().isEmpty()) {
      source.sendSystemMessage(
          Translations.of("command.gitparcel.parcel.history.empty", parcel.uuid().toString()));
      return;
    }
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.parcel.history.header",
            parcel.uuid().toString(),
            tree.nodes().size()));
    for (var node : tree.nodes()) {
      String parent = node.parentId().map(id -> id.abbreviate()).orElse("root");
      String current = tree.current().filter(node.id()::equals).isPresent() ? "*" : " ";
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.history.entry",
              current + node.id().abbreviate(),
              node.createdAt(),
              node.author(),
              node.name() + " [parent: " + parent + "]"));
    }
  }
}
