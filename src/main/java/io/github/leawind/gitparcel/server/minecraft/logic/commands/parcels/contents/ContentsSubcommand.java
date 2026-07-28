package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.contents;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ContentsSubcommand extends GitParcelBaseCommand {
  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("contents")
        .executes(
            (ctx) -> {
              var source = ctx.getSource();

              if (!validateWorldPermission(source, WorldPermissions.LIST_CONTENT_TYPES)) {
                return 0;
              }

              var registry = ParcelContentTypeRegistry.get();
              var registered = registry.registeredTypes();

              source.sendSuccess(
                  () ->
                      Translations.of(
                          "command.gitparcel.parcel.contents.list.header", registered.size()),
                  false);

              for (var type : registered) {
                boolean active = registry.latest(type.spec().id()) == type;
                source.sendSuccess(
                    () ->
                        Component.literal("  - ")
                            .append(Component.literal(type.spec().toString()))
                            .append(active ? " (active)" : ""),
                    false);
              }

              return 1;
            });
  }
}
