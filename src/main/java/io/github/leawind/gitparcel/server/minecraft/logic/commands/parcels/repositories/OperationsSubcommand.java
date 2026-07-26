package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.repositories;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.git.GitOperationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class OperationsSubcommand extends GitParcelBaseCommand {
  private static final int DEFAULT_LIMIT = 10;

  private OperationsSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("operations")
        .executes(OperationsSubcommand::list)
        .then(
            Commands.argument("id", LongArgumentType.longArg(1))
                .executes(OperationsSubcommand::show));
  }

  private static int list(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      return 0;
    }

    var operations =
        GitOperationManager.get(source.getServer()).recent(DEFAULT_LIMIT);
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.git_operation.list.header",
            operations.size()));
    operations.forEach(operation -> send(source, operation));
    return 1;
  }

  private static int show(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      return 0;
    }

    long id = LongArgumentType.getLong(ctx, "id");
    var operation = GitOperationManager.get(source.getServer()).get(id);
    if (operation.isEmpty()) {
      source.sendFailure(
          Translations.of("command.gitparcel.git_operation.not_found", id));
      return 0;
    }
    send(source, operation.orElseThrow());
    return 1;
  }

  private static void send(
      CommandSourceStack source,
      GitOperationManager.OperationSnapshot operation) {
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.git_operation.list.entry",
            operation.id(),
            operation.status().name(),
            operation.type(),
            operation.repository(),
            operation.requestedBy(),
            operation.detail()));
  }
}
