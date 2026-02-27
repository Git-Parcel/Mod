package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPalette {
  public final Map<Data, Integer> map = new HashMap<>();
  public final List<Data> list = new ArrayList<>();
  public final Set<Integer> blockEntities = new HashSet<>();

  private int nextPaletteId = 0;

  public int collect(Level level, BlockPos pos) {
    BlockState blockState = level.getBlockState(pos);
    BlockEntity blockEntity = level.getBlockEntity(pos);
    CompoundTag tag = null;
    if (blockEntity != null) {
      tag = blockEntity.saveWithFullMetadata(level.registryAccess());
    }
    return collect(blockState, tag);
  }

  public int collect(BlockState blockState, @Nullable CompoundTag nbt) {
    String blockStateString =
        BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getRegisteredName();
    return collect(blockStateString, nbt);
  }

  public int collect(String blockStateString, @Nullable CompoundTag nbt) {
    return collect(new Data(blockStateString, nbt));
  }

  public int collect(Data data) {
    if (!map.containsKey(data)) {
      map.put(data, nextPaletteId);
      list.add(data);
      if (data.nbt != null) {
        blockEntities.add(nextPaletteId);
      }
      nextPaletteId++;
      return nextPaletteId - 1;
    }
    return map.get(data);
  }

  public void clear() {
    map.clear();
    list.clear();
    nextPaletteId = 0;
  }

  public void save(Path paletteFile, Path nbtDir, boolean useSnbt) throws IOException {
    Files.createDirectories(nbtDir);
    try (BufferedWriter writer = Files.newBufferedWriter(paletteFile, StandardCharsets.UTF_8)) {
      for (int i = 0; i < list.size(); i++) {
        Data data = list.get(i);
        writer.write(Integer.toHexString(i) + "=" + data.blockStateString);
        writer.newLine();
      }
    }
    // Save NBTs
    for (int id : blockEntities) {
      Data data = list.get(id);
      if (data.nbt != null) {
        if (useSnbt) {
          // TODO format
          Files.writeString(nbtDir.resolve(id + ".snbt"), data.nbt.toString());
        } else {
          NbtIo.write(data.nbt, nbtDir.resolve(id + ".nbt"));
        }
        // TODO remove redundant NBT files
      }
    }
  }

  public record Data(String blockStateString, @Nullable CompoundTag nbt) {}

  public static BlockPalette tryLoad(Path paletteFile, Path nbtDir) throws IOException {
    BlockPalette palette = new BlockPalette();
    // TODO
    return palette;
  }
}
