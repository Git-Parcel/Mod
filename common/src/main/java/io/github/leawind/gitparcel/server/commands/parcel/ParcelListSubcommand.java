package io.github.leawind.gitparcel.server.commands.parcel;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ParcelListSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    // /parcel list format
    var parcel_list_format =
        Commands.literal("format")
            .executes(
                (ctx) -> {
                  var source = ctx.getSource();

                  if (!validateWorldPermission(source, WorldPermissions.LIST_FORMAT)) {
                    return 0;
                  }

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

    return Commands.literal("list").then(parcel_list_format);
  }
}
