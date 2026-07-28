package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.ParcelCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;

public class ConfigSubcommand extends GitParcelBaseCommand {

  public static final DynamicCommandExceptionType ERROR_INVALID_NAME =
      new DynamicCommandExceptionType(
          name -> Translations.of("command.gitparcel.parcel.config.invalid_name", name));

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var set =
        Commands.literal("set")
            .then(buildBlockSectionSize())
            .then(buildMetaName())
            .then(buildMetaAuthor())
            .then(buildMetaDescription())
            .then(buildMetaExcludeEntities())
            .then(buildVisualShowWireframe())
            .then(buildVisualShowAnchor());

    return Commands.literal("config").then(set);
  }

  private interface Setter<T> {
    void set(Parcel parcel, T value) throws CommandSyntaxException;
  }

  private static <T> int handle(
      CommandContext<CommandSourceStack> ctx,
      String key,
      ParcelValueReader<T> valueReader,
      Setter<T> setter)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.CONFIG_PARCEL)) {
      return 0;
    }
    var parcelService = ParcelService.get(source.getLevel());
    var parcels = ParcelArgument.getParcels(ctx, ParcelCommand.ARG_PARCELS);

    for (var parcel : parcels) {
      if (!validateParcelPermission(source, parcel, ParcelPermissions.CONFIG)) {
        return 0;
      }
    }

    for (var parcel : parcels) {
      var value = valueReader.read(ctx);

      setter.set(parcel, value);
      parcelService.updateParcel(parcel);

      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.config.set.success",
              parcel.uuid().toString(),
              key,
              value.toString()));
    }

    source.sendSuccess(() -> Component.literal("success"), false);
    return 1;
  }

  @FunctionalInterface
  private interface ParcelValueReader<T> {
    T read(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildBlockSectionSize() {
    return Commands.literal("content.blocks.sectionSize")
        .then(
            Commands.literal("16")
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "content.blocks.sectionSize",
                            ignored -> BlockContentType.BlockSectionSize.SIZE_16,
                            ConfigSubcommand::setBlockSectionSize)))
        .then(
            Commands.literal("32")
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "content.blocks.sectionSize",
                            ignored -> BlockContentType.BlockSectionSize.SIZE_32,
                            ConfigSubcommand::setBlockSectionSize)));
  }

  private static void setBlockSectionSize(
      Parcel parcel, BlockContentType.BlockSectionSize sectionSize) {
    var config = new BlockContentType.Config();
    ParcelContentManifest previous = parcel.meta().contents().get(BlockContentType.ID);
    if (previous != null && previous.config() != null && previous.config().isJsonObject()) {
      config.setFromJson(previous.config().getAsJsonObject());
    }
    config.sectionSize.set(sectionSize);
    var contents = new LinkedHashMap<>(parcel.meta().contents());
    contents.put(
        BlockContentType.ID,
        new ParcelContentManifest(BlockContentType.SPEC.version(), config.toJson()));
    parcel.meta().setContents(contents);
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildMetaName() {
    return Commands.literal("meta.name")
        .then(
            Commands.argument("name", StringArgumentType.string())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "meta.name",
                            c -> StringArgumentType.getString(c, "name"),
                            (parcel, name) -> {
                              try {
                                parcel.meta().setName(name);
                              } catch (IllegalArgumentException e) {
                                throw ERROR_INVALID_NAME.create(name);
                              }
                            })));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildMetaAuthor() {
    return Commands.literal("meta.author")
        .then(
            Commands.argument("author", StringArgumentType.word())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "meta.author",
                            c -> StringArgumentType.getString(c, "author"),
                            (p, v) -> p.meta().setAuthor(v))));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildMetaDescription() {
    return Commands.literal("meta.description")
        .then(
            Commands.argument("description", StringArgumentType.greedyString())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "meta.description",
                            c -> StringArgumentType.getString(c, "description"),
                            (p, v) -> p.meta().setDescription(v))));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildMetaExcludeEntities() {
    return Commands.literal("meta.excludeEntities")
        .then(
            Commands.argument("bool", BoolArgumentType.bool())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "meta.excludeEntities",
                            c -> BoolArgumentType.getBool(c, "bool"),
                            (p, v) -> p.meta().setExcludeEntities(v))));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildVisualShowWireframe() {
    return Commands.literal("visual.showWireframe")
        .then(
            Commands.argument("bool", BoolArgumentType.bool())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "visual.showWireframe",
                            c -> BoolArgumentType.getBool(c, "bool"),
                            (p, v) -> p.visual().showWireframe(v))));
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildVisualShowAnchor() {
    return Commands.literal("visual.showAnchor")
        .then(
            Commands.argument("bool", BoolArgumentType.bool())
                .executes(
                    ctx ->
                        handle(
                            ctx,
                            "visual.showAnchor",
                            c -> BoolArgumentType.getBool(c, "bool"),
                            (p, v) -> p.visual().showAnchor(v))));
  }
}
