package io.github.leawind.gitparcel.server.commands.parcel.formats;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FormatsSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var format_list =
        Commands.literal("list")
            .executes(
                (ctx) -> {
                  var source = ctx.getSource();

                  if (!validateWorldPermission(source, WorldPermissions.LIST_FORMATS)) {
                    return 0;
                  }

                  var registry = ParcelFormatRegistry.INSTANCE;

                  var savers = registry.streamSavers().toArray(ParcelFormat.Saver[]::new);
                  var loaders = registry.streamLoaders().toArray(ParcelFormat.Loader[]::new);

                  source.sendSuccess(
                      () ->
                          GitParcelTranslations.of(
                              "command.gitparcel.parcel.formats.list.header",
                              savers.length,
                              loaders.length),
                      false);

                  if (savers.length > 0) {
                    source.sendSuccess(
                        () ->
                            GitParcelTranslations.of(
                                "command.gitparcel.parcel.formats.list.savers_header"),
                        false);
                    for (var saver : savers) {
                      source.sendSuccess(
                          () ->
                              Component.literal("  - ")
                                  .append(Component.literal(saver.spec().toString())),
                          false);
                    }
                  }

                  if (loaders.length > 0) {
                    source.sendSuccess(
                        () ->
                            GitParcelTranslations.of(
                                "command.gitparcel.parcel.formats.list.loaders_header"),
                        false);
                    for (var loader : loaders) {
                      source.sendSuccess(
                          () ->
                              Component.literal("  - ")
                                  .append(Component.literal(loader.spec().toString())),
                          false);
                    }
                  }

                  return 1;
                });

    return Commands.literal("formats").then(format_list);
  }
}
