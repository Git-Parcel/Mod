package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.repositories;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import io.github.leawind.gitparcel.server.minecraft.logic.operation.OperationManager;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class OperationsSubcommand extends GitParcelBaseCommand {
  private static final int DEFAULT_LIMIT = 10;

  private OperationsSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("operations")
        .executes(OperationsSubcommand::list)
        .then(
            Commands.argument("id", StringArgumentType.word())
                .executes(OperationsSubcommand::show));
  }

  private static int list(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    boolean canManage = canManageAll(source);
    String owner = operationOwner(source);
    var operations =
        OperationManager.get(source.getServer()).recent(DEFAULT_LIMIT).stream()
            .filter(operation -> canManage || operation.owner().equals(owner))
            .toList();
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.git_operation.list.header",
            operations.size()));
    operations.forEach(operation -> send(source, operation));
    return 1;
  }

  private static int show(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    final UUID id;
    try {
      id = UUID.fromString(StringArgumentType.getString(ctx, "id"));
    } catch (IllegalArgumentException e) {
      source.sendFailure(Translations.of("command.gitparcel.git_operation.not_found", "invalid-id"));
      return 0;
    }
    var operation = OperationManager.get(source.getServer()).get(id);
    if (operation.isEmpty()) {
      source.sendFailure(
          Translations.of("command.gitparcel.git_operation.not_found", id));
      return 0;
    }
    if (!canManageAll(source)
        && !operation.orElseThrow().owner().equals(operationOwner(source))) {
      source.sendFailure(Translations.of("command.gitparcel.no_permission"));
      return 0;
    }
    send(source, operation.orElseThrow());
    return 1;
  }

  private static boolean canManageAll(CommandSourceStack source) {
    return MinecraftPermissions.permits(
        source,
        GitParcelWorldSavedData.get(source.getServer()).permissions(),
        WorldPermissions.MANAGE_SHARED_REPOSITORIES);
  }

  private static void send(
      CommandSourceStack source,
      OperationSnapshot operation) {
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.git_operation.list.entry",
            operation.operationId(),
            operation.state().name(),
            operation.kind(),
            operation.target(),
            operation.owner(),
            operation.error().or(() -> operation.result()).orElse(operation.phase())));
  }
}
