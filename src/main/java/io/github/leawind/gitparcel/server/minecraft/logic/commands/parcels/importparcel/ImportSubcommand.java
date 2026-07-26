package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcels.importparcel;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.permission.WorldPermissions;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.bind.BindSubcommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public final class ImportSubcommand extends GitParcelBaseCommand {
  private static final String ARG_REPOSITORY = "repository";
  private static final String ARG_PATH = "path";
  private static final String ARG_AT = "at";

  private ImportSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    var rotation =
        Commands.argument("rotation", TemplateRotationArgument.templateRotation())
            .executes(ImportSubcommand::importWithRotation);
    var mirror =
        Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
            .executes(ImportSubcommand::importWithMirror)
            .then(rotation);

    return Commands.literal("import")
        .then(
            Commands.argument(ARG_REPOSITORY, StringArgumentType.word())
                .suggests(BindSubcommand::suggestRepositories)
                .then(
                    Commands.argument(ARG_PATH, StringArgumentType.string())
                        .suggests(BindSubcommand::suggestParcelPaths)
                        .then(
                            Commands.argument(ARG_AT, BlockPosArgument.blockPos())
                                .executes(ImportSubcommand::importDefault)
                                .then(mirror))));
  }

  private static int importDefault(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return importParcel(ctx, Mirror.NONE, Rotation.NONE);
  }

  private static int importWithMirror(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return importParcel(
        ctx,
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        Rotation.NONE);
  }

  private static int importWithRotation(CommandContext<CommandSourceStack> ctx)
      throws CommandSyntaxException {
    return importParcel(
        ctx,
        TemplateMirrorArgument.getMirror(ctx, "mirror"),
        TemplateRotationArgument.getRotation(ctx, "rotation"));
  }

  private static int importParcel(
      CommandContext<CommandSourceStack> ctx, Mirror mirror, Rotation rotation)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    if (!validateWorldPermission(source, WorldPermissions.CREATE_PARCEL)) {
      return 0;
    }

    String repository = StringArgumentType.getString(ctx, ARG_REPOSITORY);
    String path = StringArgumentType.getString(ctx, ARG_PATH);
    BlockPos position = BlockPosArgument.getLoadedBlockPos(ctx, ARG_AT);
    try {
      var parcel =
          ParcelService.get(source.getLevel())
              .importSharedParcel(
                  repository,
                  path,
                  new ParcelTransform(mirror, rotation, position));
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.import.success",
              parcel.uuid().toString(),
              repository,
              path));
      return 1;
    } catch (Exception e) {
      LOGGER.error("Failed to import shared parcel {}/{}", repository, path, e);
      source.sendFailure(
          Translations.of(
              "command.gitparcel.parcel.import.failure",
              BindSubcommand.describe(e)));
      return 0;
    }
  }
}
