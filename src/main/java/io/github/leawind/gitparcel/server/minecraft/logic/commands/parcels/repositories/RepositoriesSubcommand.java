package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.repositories;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.git.GitOperationManager;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.shared.SharedRepositoryService;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

public final class RepositoriesSubcommand extends GitParcelBaseCommand {
  private static final String ARG_NAME = "repository";

  private RepositoriesSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var create =
        Commands.literal("create")
            .then(
                Commands.argument(ARG_NAME, StringArgumentType.word())
                    .executes(RepositoriesSubcommand::create));

    var clone =
        Commands.literal("clone")
            .then(
                Commands.argument(ARG_NAME, StringArgumentType.word())
                    .then(
                        Commands.argument("remote_url", StringArgumentType.word())
                            .executes(RepositoriesSubcommand::cloneRepository)));

    var fetch = remoteOperation("fetch", RepositoriesSubcommand::fetch);
    var pull = remoteOperation("pull", RepositoriesSubcommand::pull);
    var push = remoteOperation("push", RepositoriesSubcommand::push);

    return Commands.literal("repositories")
        .executes(RepositoriesSubcommand::list)
        .then(Commands.literal("list").executes(RepositoriesSubcommand::list))
        .then(create)
        .then(clone)
        .then(fetch)
        .then(pull)
        .then(push);
  }

  private static ArgumentBuilder<CommandSourceStack, ?> remoteOperation(
      String name,
      com.mojang.brigadier.Command<CommandSourceStack> command) {
    return Commands.literal(name)
        .then(
            Commands.argument(ARG_NAME, StringArgumentType.word())
                .suggests(RepositoriesSubcommand::suggestRepositories)
                .executes(command));
  }

  private static int list(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.LIST_SHARED_REPOSITORIES)) {
      return 0;
    }

    try {
      var repositories = SharedRepositoryService.get(source.getServer()).list();
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.repositories.list.header",
              repositories.size()));
      repositories.forEach(
          (name, info) ->
              source.sendSystemMessage(
                  Translations.of(
                      "command.gitparcel.repositories.list.entry",
                      name,
                      info.type(),
                      info.remoteUrl() == null ? "-" : info.remoteUrl(),
                      info.lastSync() == null ? "-" : info.lastSync())));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Failed to list shared repositories", e);
      source.sendFailure(
          Translations.of(
              "command.gitparcel.repositories.failure",
              describe(e)));
      return 0;
    }
  }

  private static int create(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, ARG_NAME);
    var server = ctx.getSource().getServer();
    return submit(
        ctx,
        "create",
        name,
        () -> {
          SharedRepositoryService.get(server).create(name);
          return "Created";
        });
  }

  private static int cloneRepository(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, ARG_NAME);
    String remoteUrl = StringArgumentType.getString(ctx, "remote_url");
    var server = ctx.getSource().getServer();
    return submit(
        ctx,
        "clone",
        name,
        () -> {
          SharedRepositoryService.get(server).cloneRepository(name, remoteUrl);
          return "Cloned";
        });
  }

  private static int fetch(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, ARG_NAME);
    var server = ctx.getSource().getServer();
    return submit(
        ctx,
        "fetch",
        name,
        () -> {
          int updates =
              SharedRepositoryService.get(server).fetch(name);
          return updates + " tracking reference(s) updated";
        });
  }

  private static int pull(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, ARG_NAME);
    var server = ctx.getSource().getServer();
    return submit(
        ctx,
        "pull",
        name,
        () -> SharedRepositoryService.get(server).pull(name));
  }

  private static int push(CommandContext<CommandSourceStack> ctx) {
    String name = StringArgumentType.getString(ctx, ARG_NAME);
    var server = ctx.getSource().getServer();
    return submit(
        ctx,
        "push",
        name,
        () -> {
          int updates =
              SharedRepositoryService.get(server).push(name);
          return updates + " remote reference(s) updated";
        });
  }

  private static int submit(
      CommandContext<CommandSourceStack> ctx,
      String type,
      String repository,
      Callable<String> action) {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.MANAGE_SHARED_REPOSITORIES)) {
      return 0;
    }

    var operation =
        GitOperationManager.get(source.getServer())
            .submit(
                type,
                repository,
                source.getTextName(),
                action,
                completed -> {
                  if (completed.status() == GitOperationManager.Status.SUCCEEDED) {
                    source.sendSystemMessage(
                        Translations.of(
                            "command.gitparcel.git_operation.success",
                            completed.id(),
                            completed.type(),
                            completed.repository(),
                            completed.detail()));
                  } else {
                    source.sendFailure(
                        Translations.of(
                            "command.gitparcel.git_operation.failure",
                            completed.id(),
                            completed.type(),
                            completed.repository(),
                            completed.detail()));
                  }
                });
    source.sendSystemMessage(
        Translations.of(
            "command.gitparcel.git_operation.started",
            operation.id(),
            type,
            repository));
    return operation.status() == GitOperationManager.Status.FAILED ? 0 : 1;
  }

  private static CompletableFuture<Suggestions> suggestRepositories(
      CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
    try {
      return SharedSuggestionProvider.suggest(
          SharedRepositoryService.get(ctx.getSource().getServer()).list().keySet(),
          builder);
    } catch (IOException e) {
      return Suggestions.empty();
    }
  }

  private static String describe(Exception exception) {
    String message = exception.getMessage();
    return exception.getClass().getSimpleName()
        + (message == null ? "" : ": " + message);
  }
}
