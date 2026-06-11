package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.BlockPalette;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.Subparcel;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Saver;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
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
      throws IOException, ParcelException.UnsupportedFeature {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcelSize, anchor, transform, dataDir, ignoreEntities, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER)) {

      saveBlocks(ctx, 16);

      if (!ignoreEntities) {
        saveEntities(ctx, problemReporter);
      }
    }
  }

  @Override
  protected void writeSubparcelRLE3D(
      Context ctx, Path file, Subparcel subparcel, List<BlockEntityEntry> blockEntities)
      throws IOException {
    var sb = new StringBuilder(8192);
    char[] hexChars = HexUtils.UPPERS;

    BlockPalette palette = ctx.blockPalette;
    var level = ctx.level;
    var transform = ctx.transform;

    // When no palette, use a temporary identity map for VolumetricRLE int IDs
    var stateToTempId = new IdentityHashMap<BlockState, Integer>();
    var tempIdToState = new ArrayList<BlockState>();

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

              // get blockState in world space
              BlockState blockState = level.getBlockState(pos);
              // convert blockState to local space
              blockState = transform.applyInverted(blockState);

              BlockEntity blockEntity = level.getBlockEntity(pos);
              if (blockEntity != null) {
                CompoundTag nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
                blockEntities.add(new BlockEntityEntry(new BlockPos(x, y, z), nbt));
              }

              if (palette != null) {
                return palette.collect(blockState);
              }

              return stateToTempId.computeIfAbsent(
                  blockState,
                  k -> {
                    int id = tempIdToState.size();
                    tempIdToState.add(k);
                    return id;
                  });
            });

    for (var run : runs) {
      sb.append(hexChars[run.minX()]).append(hexChars[run.minY()]).append(hexChars[run.minZ()]);

      int maxX = run.maxX();
      int maxY = run.maxY();
      int maxZ = run.maxZ();

      if (run.minX() != maxX || run.minY() != maxY || run.minZ() != maxZ) {
        sb.append(hexChars[maxX]).append(hexChars[maxY]).append(hexChars[maxZ]);
      }

      sb.append(palette != null ? '~' : '=');
      if (palette != null) {
        sb.append(HexUtils.toHexUpperCase(run.value()));
      } else {
        sb.append(BlockPalette.stringifyBlockState(tempIdToState.get(run.value())));
      }
      sb.append('\n');
    }

    Files.writeString(file, sb, StandardCharsets.UTF_8);
  }
}
