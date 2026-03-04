package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.config.BooleanConfigItem;
import io.github.leawind.gitparcel.api.parcel.config.EnumConfigItem;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import net.minecraft.core.Vec3i;

public interface ParcellaD16Format extends ParcelFormat.Impl<ParcellaD16Format.Config> {
  String BLOCKS_DIR_NAME = "blocks";
  String ENTITIES_DIR_NAME = "entities";
  String NBT_DIR_NAME = "nbt";
  String PALETTE_FILE_NAME = "palette.txt";
  String SUBPARCELS_DIR_NAME = "subparcels";
  String SUBPARCEL_SUFFIX = ".txt";

  @Override
  default String id() {
    return "parcella_d16";
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
    public BooleanConfigItem enableMicroparcel =
        new BooleanConfigItem("enableMicroparcel").defaultValue(true).storeRightHere();

    public Config() {
      register(blockEntityDataFormat).register(entityDataFormat).register(enableMicroparcel);
    }
  }
}
