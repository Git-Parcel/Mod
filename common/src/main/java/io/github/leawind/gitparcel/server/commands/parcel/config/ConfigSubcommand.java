package io.github.leawind.gitparcel.server.commands.parcel.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.commands.arguments.ParcelArgument;
import io.github.leawind.gitparcel.commands.arguments.ParcelFormatArgument;
import io.github.leawind.gitparcel.permission.WorldPermissions;
import io.github.leawind.gitparcel.server.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.world.Parcel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ConfigSubcommand extends GitParcelBaseCommand {

  public static final DynamicCommandExceptionType ERROR_INVALID_NAME =
      new DynamicCommandExceptionType(
          name -> GitParcelTranslations.of("command.gitparcel.parcel.config.invalid_name", name));

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var set =
        Commands.literal("set")
            .then(buildMetaFormat())
            .then(buildMetaName())
            .then(buildMetaAuthor())
            .then(buildMetaDescription())
            .then(buildMetaExcludeEntities())
            .then(buildVisualShowWireframe())
            .then(buildVisualShowAnchor());

    var parcel = Commands.argument("parcel", ParcelArgument.singleParcel()).then(set);

    return Commands.literal("config").then(parcel);
  }

  // ////////////////////////////////////////////////////////////////
  // Helpers
  // ////////////////////////////////////////////////////////////////

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

    var parcel = ParcelArgument.getSingleParcel(ctx, "parcel");
    var value = valueReader.read(ctx);

    setter.set(parcel, value);
    parcel.emitUpdate();

    source.sendSuccess(
        () ->
            GitParcelTranslations.of(
                "command.gitparcel.parcel.config.set.success",
                parcel.uuid().toString(),
                key,
                value.toString()),
        false);
    return 1;
  }

  @FunctionalInterface
  private interface ParcelValueReader<T> {
    T read(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
  }

  // ////////////////////////////////////////////////////////////////
  // meta
  // ////////////////////////////////////////////////////////////////

  private static ArgumentBuilder<CommandSourceStack, ?> buildMetaFormat() {
    return Commands.literal("meta.format")
        .then(
            Commands.argument("save_format", ParcelFormatArgument.saver())
                .executes(
                    ctx -> {
                      var saver = ParcelFormatArgument.getSaver(ctx, "save_format");
                      return handle(
                          ctx,
                          "meta.format",
                          c -> saver,
                          (p, s) -> p.meta().setFormatInfo(s.info()));
                    }));
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

  // ////////////////////////////////////////////////////////////////
  // visual
  // ////////////////////////////////////////////////////////////////

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
