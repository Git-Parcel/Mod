package io.github.leawind.gitparcel.parcel.formats.mvp;

import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MvpV0 implements ParcelFormat {

  @Override
  public String id() {
    return "mvp";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends MvpV0 implements ParcelFormat.Save {
    @Override
    public void save(Level level, BlockPos from, Vec3i size, Path dir, boolean saveEntities)
        throws IOException {
      Files.createDirectories(dir);

      // Create blocks directory structure
      Path blocksDir = dir.resolve("blocks");
      Path paletteFile = blocksDir.resolve("palette.txt");
      Path nbtDir = blocksDir.resolve("nbt");
      Path subParcelsDir = blocksDir.resolve("subparcels");

      // Create palette to map block states to IDs
      Map<BlockState, Integer> palette = new HashMap<>();
      List<BlockState> paletteList = new ArrayList<>();
      int nextPaletteId = 0;

      // First pass: collect all unique block states and build palette
      for (int x = 0; x < size.getX(); x++) {
        for (int y = 0; y < size.getY(); y++) {
          for (int z = 0; z < size.getZ(); z++) {
            BlockPos pos = from.offset(x, y, z);
            BlockState blockState = level.getBlockState(pos);

            if (!palette.containsKey(blockState)) {
              palette.put(blockState, nextPaletteId);
              paletteList.add(blockState);
              nextPaletteId++;
            }
          }
        }
      }

      // Write palette to file
      Files.createDirectories(paletteFile.getParent());
      try (BufferedWriter writer = Files.newBufferedWriter(paletteFile, StandardCharsets.UTF_8)) {
        for (int i = 0; i < paletteList.size(); i++) {
          BlockState blockState = paletteList.get(i);
          String blockStateString =
              BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getRegisteredName();
          writer.write(Integer.toHexString(i) + "=" + blockStateString);
          writer.newLine();
        }
      }

      // Process blocks in sub-parcels of max 16x16x16
      int subSize = 16; // Maximum size for sub-parcels

      // Calculate total number of sub-parcels in each dimension
      int subParcelCountX = (size.getX() + subSize - 1) / subSize;
      int subParcelCountY = (size.getY() + subSize - 1) / subSize;
      int subParcelCountZ = (size.getZ() + subSize - 1) / subSize;
      int totalSubParcels = subParcelCountX * subParcelCountY * subParcelCountZ;

      // Calculate the number of digits needed for indexing
      int maxIndex = totalSubParcels - 1;
      int totalDigitsNeeded =
          (maxIndex == 0) ? 1 : (int) Math.ceil(Math.log(maxIndex + 1) / Math.log(16));
      // Ensure the number of digits is even
      if (totalDigitsNeeded % 2 == 1) {
        totalDigitsNeeded++;
      }

      for (int sx = 0; sx < subParcelCountX; sx++) {
        for (int sy = 0; sy < subParcelCountY; sy++) {
          for (int sz = 0; sz < subParcelCountZ; sz++) {
            // Calculate actual bounds for this sub-parcel
            int startX = sx * subSize;
            int startY = sy * subSize;
            int startZ = sz * subSize;
            int endX = Math.min(startX + subSize, size.getX());
            int endY = Math.min(startY + subSize, size.getY());
            int endZ = Math.min(startZ + subSize, size.getZ());

            // Calculate one-dimensional index
            int currentIndex = sx * subParcelCountY * subParcelCountZ + sy * subParcelCountZ + sz;

            // Convert to hex string with leading zeros to ensure consistent length
            String hexIndex = String.format("%0" + totalDigitsNeeded + "X", currentIndex);

            // Create hierarchical path: split the hex string every 2 characters
            Path subParcelPath = subParcelsDir;
            for (int i = 0; i < hexIndex.length() - 2; i += 2) {
              String segment = hexIndex.substring(i, Math.min(i + 2, hexIndex.length()));
              subParcelPath = subParcelPath.resolve(segment);
            }

            // Create sub-parcel file with hex index as filename
            Path subParcelFile = subParcelPath.resolve(hexIndex + ".txt");

            // Create parent directories if they don't exist
            Files.createDirectories(subParcelFile.getParent());

            try (BufferedWriter writer =
                Files.newBufferedWriter(subParcelFile, StandardCharsets.UTF_8)) {
              // Write block palette indices for this sub-parcel
              for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                  for (int z = startZ; z < endZ; z++) {
                    BlockPos pos = from.offset(x, y, z);
                    BlockState blockState = level.getBlockState(pos);
                    int paletteId = palette.get(blockState);
                    writer.write(Integer.toHexString(paletteId));
                    writer.newLine();
                  }
                }
              }
            }
          }
        }
      }

      Files.createDirectories(nbtDir);
      // Handle block entities - save them as individual SNBT files in nbt directory
      for (int x = 0; x < size.getX(); x++) {
        for (int y = 0; y < size.getY(); y++) {
          for (int z = 0; z < size.getZ(); z++) {
            BlockPos pos = from.offset(x, y, z);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
              // Find the corresponding block state to get its palette ID
              BlockState blockState = level.getBlockState(pos);
              int paletteId = palette.get(blockState);

              // Save block entity NBT data
              CompoundTag tag = blockEntity.saveWithFullMetadata(level.registryAccess());
              Path nbtFile = nbtDir.resolve(paletteId + ".snbt");

              // Only write if the file doesn't exist to avoid overwriting
              if (!Files.exists(nbtFile)) {
                try (BufferedWriter writer =
                    Files.newBufferedWriter(nbtFile, StandardCharsets.UTF_8)) {
                  // Convert tag to SNBT (String NBT) format
                  String snbt = tag.toString();
                  writer.write(snbt);
                }
              }
            }
          }
        }
      }
    }
  }
}
