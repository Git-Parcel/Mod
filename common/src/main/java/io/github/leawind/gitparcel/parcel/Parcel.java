package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.Constants;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class Parcel {
  private final ServerLevel level;
  private BoundingBox bounds;

  public Parcel(ServerLevel level, BoundingBox bounds) {
    this.level = level;
    this.bounds = bounds;
  }

  public ServerLevel getLevel() {
    return level;
  }

  public BoundingBox getBounds() {
    return bounds;
  }

  void setBounds(BoundingBox bounds) {
    this.bounds = bounds;
  }

  public Vec3i getSize() {
    return new Vec3i(bounds.getXSpan(), bounds.getYSpan(), bounds.getZSpan());
  }

  public BlockPos getFromCorner() {
    return new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
  }

  public BlockPos getToCorner() {
    return new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ());
  }

  /**
   * Metadata for parcels.
   *
   * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
   */
  public static final class Metadata {
    public String format;
    public int dataVersion;
    public Vec3i size;
    public String name;
    public String author;
    public String description = "";
    public List<String> tags = new ArrayList<>();
    public List<ModDependency> mods = new ArrayList<>();
    public boolean includeBlock = true;
    public boolean includeEntity = true;

    public Metadata(String format, int dataVersion, Vec3i size, String name, String author) {
      this.format = format;
      this.dataVersion = dataVersion;
      this.size = size;
      this.name = name;
      this.author = author;
      SharedConstants.getCurrentVersion().dataVersion();
    }

    public record ModDependency(String id, String min, String max) {}

    public static void save(Parcel.Metadata metadata, Path dir) {
      // TODO
      Constants.LOG.warn("Saving metadata of metadata {} to {}", metadata, dir);
    }

    public static Metadata load(Path dir) {
      Constants.LOG.warn("Loading metadata from {}", dir);
      // TODO
      return null;
    }
  }
}
