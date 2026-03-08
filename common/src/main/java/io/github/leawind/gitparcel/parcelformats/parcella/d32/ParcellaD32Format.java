package io.github.leawind.gitparcel.parcelformats.parcella.d32;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.config.EnumConfigItem;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.SubparcelFormat;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;

public interface ParcellaD32Format extends ParcelFormat.Impl<ParcellaD32Format.Config> {
  String BLOCKS_DIR_NAME = "blocks";
  String ENTITIES_DIR_NAME = "entities";
  String NBT_DIR_NAME = "nbt";
  String PALETTE_FILE_NAME = "palette.txt";
  String SUBPARCELS_DIR_NAME = "subparcels";
  String SUBPARCEL_SUFFIX = ".txt";

  @Override
  default String id() {
    return "parcella_d32";
  }

  @Override
  default int version() {
    return 0;
  }

  @Override
  default <T> Config castConfig(T config) throws ClassCastException {
    return (Config) config;
  }

  @Override
  default Config getDefaultConfig() {
    return new Config();
  }

  class Config extends ParcelFormatConfig<Config> {
    private static final String SCHEMA_URL =
        "https://git-parcel.github.io/schemas/ParcellaFormatConfig.json";

    public Vec3i anchorOffset = Vec3i.ZERO;

    public EnumConfigItem<NbtFormat> blockEntityDataFormat =
        new EnumConfigItem<>(NbtFormat.class, "blockEntityDataFormat")
            .defaultValue(NbtFormat.Text)
            .storeRightHere();
    public EnumConfigItem<NbtFormat> entityDataFormat =
        new EnumConfigItem<>(NbtFormat.class, "entityDataFormat")
            .defaultValue(NbtFormat.Text)
            .storeRightHere();
    public EnumConfigItem<SubparcelFormat> subparcelFormat =
        new EnumConfigItem<>(SubparcelFormat.class, "subparcelFormat")
            .defaultValue(SubparcelFormat.RLE3D)
            .storeRightHere();

    public Config() {
      register(blockEntityDataFormat).register(entityDataFormat).register(subparcelFormat);
    }
  }

  interface EntityNbtFilePath {
    static Path resolve(Path nbtDir, NbtFormat nbtFormat, int entityId) {
      return nbtDir.resolve(entityId + nbtFormat.suffix);
    }

    /**
     * Parse entity ID from NBT file path.
     *
     * @return Entity ID parsed from file name, or -1 if not valid
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
}
