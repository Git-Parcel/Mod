package io.github.leawind.gitparcel.parcel;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;

/** A format for saving or loading parcels. */
public interface ParcelFormat {
  default boolean isSaver() {
    throw new UnsupportedOperationException("Method not implemented");
  }

  default boolean isLoader() {
    throw new UnsupportedOperationException("Method not implemented");
  }

  /** Unique id of the format. */
  String id();

  /** Version of the format. */
  int version();

  interface Save extends ParcelFormat {
    @Override
    default boolean isSaver() {
      return true;
    }

    @Override
    default boolean isLoader() {
      return false;
    }

    /**
     * Save parcel content to directory
     *
     * @param level Level
     * @param from Position in level
     * @param size Size of the parcel
     * @param dir Path to an existing directory
     * @param includeBlock Whether to include blocks in the parcel
     * @param includeEntity Whether to include entities in the parcel
     */
    void save(
        ServerLevel level,
        BlockPos from,
        Vec3i size,
        Path dir,
        boolean includeBlock,
        boolean includeEntity)
        throws IOException;
  }

  interface Load extends ParcelFormat {
    @Override
    default boolean isSaver() {
      return false;
    }

    @Override
    default boolean isLoader() {
      return true;
    }

    /**
     * Load parcel content from directory
     *
     * @param level Level
     * @param pos Position in level
     * @param dir Path to parcel directory, must exist
     * @param options Loading options
     */
    void load(ServerLevel level, BlockPos pos, Path dir, Options options) throws IOException;

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
