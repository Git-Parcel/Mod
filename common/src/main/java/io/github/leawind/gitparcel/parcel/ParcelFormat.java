package io.github.leawind.gitparcel.parcel;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/** A format for saving or loading parcels. */
public interface ParcelFormat {
  Logger LOGGER = LogUtils.getLogger();

  /** Unique id of the format. */
  String id();

  /** Version of the format. */
  int version();

  interface Save extends ParcelFormat {

    /**
     * Save parcel content to directory
     *
     * @param level Level
     * @param from Position in level
     * @param size Size of the parcel
     * @param dir Path to an existing directory
     * @param saveBlocks Whether to save blocks in the parcel
     * @param saveEntities Whether to save entities in the parcel
     */
    void save(
        Level level, BlockPos from, Vec3i size, Path dir, boolean saveBlocks, boolean saveEntities)
        throws IOException;
  }

  interface Load extends ParcelFormat {

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param pos Position in level
     * @param dir Path to parcel directory, must exist
     * @param options Loading options
     */
    void load(Level level, BlockPos pos, Path dir, Options options) throws IOException;

    /**
     * Options for loading parcels.
     *
     * @param includeBlock Whether to load blocks
     * @param includeEntity Whether to load entities
     * @param clearEntities If load entities, whether to clear entities in the parcel before loading
     */
    record Options(boolean includeBlock, boolean includeEntity, boolean clearEntities) {}
  }
}
