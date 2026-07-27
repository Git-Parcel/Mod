package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import io.github.leawind.gitparcel.common.api.config.ConfigItemBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponent;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponents;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/** Configuration and storage conventions shared by all Parcella grid variants. */
public interface ParcellaFormat extends ParcelFormat.Impl<ParcellaFormat.Config> {
  String BLOCKS_DIR_NAME = "blocks";
  String ENTITIES_DIR_NAME = "entities";
  String ATTACHMENTS_DIR_NAME = "attachments";
  String PALETTE_FILE_NAME = "palette.txt";
  String SECTIONS_DIR_NAME = "sections";
  String SECTION_BLOCK_STATE_SUFFIX = ".txt";
  String SECTION_BLOCK_ENTITY_SUFFIX = ".be.snbt";

  @Override
  default Set<ParcelDataComponent> dataComponents() {
    return Set.of(
        ParcelDataComponents.BLOCKS,
        ParcelDataComponents.ENTITIES,
        ParcelDataComponents.ATTACHMENTS);
  }

  @Override
  default EnumSet<Feature> features() {
    return EnumSet.allOf(Feature.class);
  }

  @Override
  default <T> @NonNull Config castConfig(@NonNull T config) throws ClassCastException {
    return (Config) config;
  }

  @Override
  default Class<Config> configClass() {
    return Config.class;
  }

  @Override
  default Config getDefaultConfig() {
    return new Config();
  }

  final class Config extends ParcelFormatConfig<Config> {
    private static final String SCHEMA_URL =
        "https://git-parcel.github.io/schemas/ParcellaFormatConfig.json";

    public final ConfigItem<NbtFormat> blockEntityDataFormat =
        ConfigItemBuilder.ofEnum("blockEntityDataFormat", NbtFormat.TEXT).storeLocally().build();
    public final ConfigItem<NbtFormat> entityDataFormat =
        ConfigItemBuilder.ofEnum("entityDataFormat", NbtFormat.TEXT).storeLocally().build();
    public final ConfigItem<BlockStateEncoding> blockStateEncoding =
        ConfigItemBuilder.ofEnum("blockStateEncoding", BlockStateEncoding.RLE3D)
            .storeLocally()
            .build();

    /**
     * Whether block states are stored in a shared palette and referenced by short IDs.
     */
    public final ConfigItem<Boolean> usePalette =
        ConfigItemBuilder.ofBoolean("usePalette").defaultValue(true).storeLocally().build();

    public Config() {
      register(blockEntityDataFormat);
      register(entityDataFormat);
      register(blockStateEncoding);
      register(usePalette);
    }
  }
}
