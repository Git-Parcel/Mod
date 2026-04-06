package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
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

public class ParcellaD16Saver extends ParcellaD32Saver
    implements ParcellaD16Format, ParcelFormat.Saver<ParcellaD32Format.Config> {
  @Override
  public void save(
      Level level,
      Vec3i parcelSize,
      Vec3i anchor,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreEntities,
      @Nullable Config config)
      throws IOException {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcelSize, anchor, transform, dataDir, ignoreEntities, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(LOGGER)) {

      saveBlocks(ctx, 16);

      if (!ignoreEntities) {
        saveEntities(ctx, problemReporter);
      }
    }
  }

  @Override
  protected void writeSubparcelRLE3D(Context ctx, Path file, Subparcel subparcel)
      throws IOException {
    var sb = new StringBuilder(8192);
    char[] hexChars = HexUtils.UPPERS;

    var level = ctx.level;
    var palette = ctx.blockPalette;
    var transform = ctx.transform;

    var runs =
        VolumetricRLE.IMPL.encode(
            subparcel.sizeX,
            subparcel.sizeY,
            subparcel.sizeZ,
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
      sb.append(hexChars[run.minX()]).append(hexChars[run.minY()]).append(hexChars[run.minZ()]);

      int maxX = run.maxX();
      int maxY = run.maxY();
      int maxZ = run.maxZ();

      if (run.minX() != maxX || run.minY() != maxY || run.minZ() != maxZ) {
        sb.append(hexChars[maxX]).append(hexChars[maxY]).append(hexChars[maxZ]);
      }

      sb.append('=').append(HexUtils.toHexUpperCase(run.value())).append('\n');
    }

    Files.writeString(file, sb, StandardCharsets.UTF_8);
  }
}
