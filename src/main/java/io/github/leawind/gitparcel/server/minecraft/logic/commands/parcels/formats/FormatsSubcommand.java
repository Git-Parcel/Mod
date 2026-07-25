package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.formats;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class FormatsSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("formats")
        .executes(
            (ctx) -> {
              var source = ctx.getSource();

              if (!validateWorldPermission(source, WorldPermissions.LIST_FORMATS)) {
                return 0;
              }

              var registry = ParcelFormatRegistry.get();

              var writers = registry.streamWriters().toArray(ParcelFormat.Writer[]::new);
              var readers = registry.streamReaders().toArray(ParcelFormat.Reader[]::new);

              source.sendSuccess(
                  () ->
                      Translations.of(
                          "command.gitparcel.parcel.formats.list.header",
                          writers.length,
                          readers.length),
                  false);

              if (writers.length > 0) {
                source.sendSuccess(
                    () -> Translations.of("command.gitparcel.parcel.formats.list.writers_header"),
                    false);
                for (var writer : writers) {
                  source.sendSuccess(
                      () ->
                          Component.literal("  - ")
                              .append(Component.literal(writer.spec().toString())),
                      false);
                }
              }

              if (readers.length > 0) {
                source.sendSuccess(
                    () -> Translations.of("command.gitparcel.parcel.formats.list.readers_header"),
                    false);
                for (var reader : readers) {
                  source.sendSuccess(
                      () ->
                          Component.literal("  - ")
                              .append(Component.literal(reader.spec().toString())),
                      false);
                }
              }

              return 1;
            });
  }
}
