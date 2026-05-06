package io.github.leawind.gitparcel.parcelformats.parcella.d32;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.config.ConfigItem;
import io.github.leawind.gitparcel.api.parcel.config.ConfigItemBuilder;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.SubparcelFormat;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.NonNull;

public interface ParcellaD32Format extends ParcelFormat.Impl<ParcellaD32Format.Config> {
  String BLOCKS_DIR_NAME = "blocks";
  String ENTITIES_DIR_NAME = "entities";
  String PALETTE_FILE_NAME = "palette.txt";
  String SUBPARCELS_DIR_NAME = "subparcels";
  String SUBPARCEL_BLOCK_STATE_SUFFIX = ".txt";
  String SUBPARCEL_BLOCK_ENTITY_SUFFIX = ".be.snbt";

  Spec SPEC = new Spec("parcella_d32", 0);

  @Override
  default Spec spec() {
    return SPEC;
  }

  @Override
  default EnumSet<Feature> features() {
    return EnumSet.of(Feature.ROTATE, Feature.MIRROR);
  }

  @Override
  default <T> @NonNull Config castConfig(@NonNull T config) throws ClassCastException {
    return (Config) config;
  }

  @Override
  default Config getDefaultConfig() {
    return new Config();
  }

  class Config extends ParcelFormatConfig<Config> {
    private static final String SCHEMA_URL =
        "https://git-parcel.github.io/schemas/ParcellaFormatConfig.json";

    public ConfigItem<NbtFormat> blockEntityDataFormat =
        ConfigItemBuilder.ofEnum("blockEntityDataFormat", NbtFormat.Text).storeLocally().build();
    public ConfigItem<NbtFormat> entityDataFormat =
        ConfigItemBuilder.ofEnum("entityDataFormat", NbtFormat.Text).storeLocally().build();
    public ConfigItem<SubparcelFormat> subparcelFormat =
        ConfigItemBuilder.ofEnum("subparcelFormat", SubparcelFormat.RLE3D).storeLocally().build();

    public Config() {
      register(blockEntityDataFormat);
      register(entityDataFormat);
      register(subparcelFormat);
    }
  }

  interface EntityNbtFilePath {
    static Path resolve(Path nbtDir, NbtFormat nbtFormat, int entityId) {
      return nbtDir.resolve(entityId + nbtFormat.suffix);
    }

    /**
     * Parse entity ID from NBT file path.
     *
     * @return Entity ID parsed from file key, or -1 if not valid
     */
    static int fromPath(Path nbtFile, NbtFormat nbtFormat) {
      String fileName = nbtFile.getFileName().toString();
      String suffix = nbtFormat.suffix;
      if (!fileName.endsWith(suffix)) {
        return -1;
      }
      String idPart = fileName.substring(0, fileName.length() - suffix.length());
      try {
        return Integer.parseInt(idPart);
      } catch (NumberFormatException e) {
        return -1;
      }
    }
  }

  /**
   * @param pos Local position
   */
  record BlockEntityEntry(BlockPos pos, CompoundTag data) {
    public static Codec<BlockEntityEntry> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(BlockEntityEntry::pos),
                        CompoundTag.CODEC.fieldOf("data").forGetter(BlockEntityEntry::data))
                    .apply(inst, BlockEntityEntry::new));

    /** Compare by (y, x, z) for deterministic ordering. */
    public static final Comparator<BlockEntityEntry> COMPARATOR =
        Comparator.comparingInt(BlockEntityEntry::y)
            .thenComparingInt(BlockEntityEntry::x)
            .thenComparingInt(BlockEntityEntry::z);

    int x() {
      return pos.getX();
    }

    int y() {
      return pos.getY();
    }

    int z() {
      return pos.getZ();
    }
  }

  record BlockEntities(List<BlockEntityEntry> blockEntities) {
    public static Codec<BlockEntities> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        BlockEntityEntry.CODEC
                            .listOf()
                            .fieldOf("blockEntities")
                            .forGetter(BlockEntities::blockEntities))
                    .apply(inst, BlockEntities::new));
  }
}
