package io.github.leawind.gitparcel.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.world.gitparcel.GitParcelSavedData;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ParcelCommand {
  public static void register(
      CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
    // /parcel list
    var parcel_list = Commands.literal("list");

    // /parcel list format
    var parcel_list_format =
        Commands.literal("format")
            .executes(
                (ctx) -> {
                  var source = ctx.getSource();
                  var registry = ParcelFormatRegistry.INSTANCE;

                  List<ParcelFormat.Info> saverInfos = registry.getSaverInfos();
                  List<ParcelFormat.Info> loaderInfos = registry.getLoaderInfos();

                  source.sendSuccess(
                      () ->
                          GitParcelTranslations.of(
                              "command.parcel.list.format.header",
                              saverInfos.size(),
                              loaderInfos.size()),
                      false);

                  if (!saverInfos.isEmpty()) {
                    source.sendSuccess(
                        () -> GitParcelTranslations.of("command.parcel.list.format.savers_header"),
                        false);
                    for (var info : saverInfos) {
                      source.sendSuccess(
                          () ->
                              Component.literal("  - ").append(Component.literal(info.toString())),
                          false);
                    }
                  }

                  if (!loaderInfos.isEmpty()) {
                    source.sendSuccess(
                        () -> GitParcelTranslations.of("command.parcel.list.format.loaders_header"),
                        false);
                    for (var info : loaderInfos) {
                      source.sendSuccess(
                          () ->
                              Component.literal("  - ").append(Component.literal(info.toString())),
                          false);
                    }
                  }

                  return 1;
                });

    // /parcel list parcel_instance
    var parcel_list_parcel_instance =
        Commands.literal("parcel_instance")
            .executes(
                (ctx) -> {
                  var source = ctx.getSource();
                  var level = source.getLevel();
                  var savedData = GitParcelSavedData.get(level);
                  List<ParcelInstance> instances = savedData.listParcelInstances();

                  source.sendSuccess(
                      () ->
                          GitParcelTranslations.of(
                              "command.parcel.list.parcel_instance.header", instances.size()),
                      false);

                  for (var instance : instances) {
                    source.sendSuccess(
                        () ->
                            Component.literal("  - UUID: ")
                                .append(Component.literal(instance.uuid().toString()))
                                .append(Component.literal(", Box: "))
                                .append(Component.literal(instance.boundingBox().toString()))
                                .append(Component.literal(", Rotation: "))
                                .append(Component.literal(instance.rotation().name()))
                                .append(Component.literal(", Mirror: "))
                                .append(Component.literal(instance.mirror().name())),
                        false);
                  }

                  return 1;
                });

    // Build the command tree
    var parcel_list_with_sub =
        parcel_list.then(parcel_list_format).then(parcel_list_parcel_instance);

    final var command =
        Commands.literal("parcel")
            .executes(
                (ctx) -> {
                  GitParcelMod.LOGGER.info("parcel command executed!");
                  return 0;
                })
            .then(parcel_list_with_sub);

    dispatcher.register(command);
  }
}
