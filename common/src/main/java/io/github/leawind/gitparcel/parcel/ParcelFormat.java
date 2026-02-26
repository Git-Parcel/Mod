package io.github.leawind.gitparcel.parcel;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
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
     * @param parcelOrigin Position of parcel origin in level
     * @param parcelSize Size of the parcel, must be positive
     * @param dataDir Path to parcel data directory
     * @param saveEntities Whether to save entities in the parcel
     */
    void save(
        Level level, BlockPos parcelOrigin, Vec3i parcelSize, Path dataDir, boolean saveEntities)
        throws IOException;
  }

  interface Load extends ParcelFormat {

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param parcelOrigin Position of parcel origin in level
     * @param dataDir Path to parcel data directory
     * @param loadBlocks Whether to load blocks
     * @param loadEntities Whether to load entities
     */
    void load(
        ServerLevel level,
        BlockPos parcelOrigin,
        Path dataDir,
        boolean loadBlocks,
        boolean loadEntities)
        throws IOException, ParcelException;
  }
}
