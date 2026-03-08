package io.github.leawind.gitparcel.parcelformats.parcella.d32;

import io.github.leawind.gitparcel.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Format;
import io.github.leawind.gitparcel.parcelformats.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.utils.numbase.Base32Utils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ParcellaD32Saver extends ParcellaD16Saver
    implements ParcellaD32Format, ParcelFormat.Save<ParcellaD16Format.Config> {
  @Override
  public void save(
      Level level,
      Vec3i parcelSize,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreEntities,
      @Nullable Config config)
      throws IOException {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcelSize, transform, dataDir, ignoreEntities, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(LOGGER)) {

      saveBlocks(ctx, 32);

      if (!ignoreEntities) {
        saveEntities(ctx, problemReporter);
      }
    }
  }

  @Override
  protected void writeSubparcelRLE3D(Context ctx, Path file, Subparcel subparcel)
      throws IOException {
    var sb = new StringBuilder(8192);
    char[] chars = Base32Utils.BASE32_DIGITS;

    var level = ctx.level;
    var palette = ctx.blockPalette;
    var transform = ctx.transform;

    var runs =
        VolumetricRLE.IMPL.encode(
            ctx.parcelSize.getX(),
            ctx.parcelSize.getY(),
            ctx.parcelSize.getZ(),
            (x, y, z) -> {
              BlockPos pos =
                  new BlockPos(x + subparcel.originX, y + subparcel.originY, z + subparcel.originZ);
              pos = ctx.transform.apply(pos);
              // pos: world space

              BlockState blockState = level.getBlockState(pos);
              // blockState: world space
              blockState = transform.applyInverted(blockState);
              // blockState: local space
              BlockEntity blockEntity = level.getBlockEntity(pos);
              CompoundTag nbt = null;
              if (blockEntity != null) {
                nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
              }

              return palette.collect(blockState, nbt);
            });

    for (var run : runs) {
      sb.append(chars[run.minX()]).append(chars[run.minY()]).append(chars[run.minZ()]);

      int maxX = run.maxX();
      int maxY = run.maxY();
      int maxZ = run.maxZ();

      if (run.minX() != maxX || run.minY() != maxY || run.minZ() != maxZ) {
        sb.append(chars[maxX]).append(chars[maxY]).append(chars[maxZ]);
      }

      sb.append('=').append(Base32Utils.toBase32(run.value())).append('\n');
    }

    Files.writeString(file, sb, StandardCharsets.UTF_8);
  }
}
