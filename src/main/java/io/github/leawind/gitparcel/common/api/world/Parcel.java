package io.github.leawind.gitparcel.common.api.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/// Parcel represents an axially aligned cuboid area in the world.
///
/// - The anchor and placement transform are treated as points, not block positions.
/// - The anchor is a persistent reference point stored as absolute world coordinates. It does not
///   move when the parcel bounds change, and archive content is addressed relative to it.
///
/// ### Demo
///
/// In this demo, we omit the y coordinate and focus on `(x, z)`.
///
/// Direction:
///
/// ```txt
///  o----> +x
///  |
///  ↓
///  +z
/// ```
///
/// Lets's say we have a parcel:
///
/// - Size`(x, z)` = `(3, 5)`
/// - Anchor point is `(0, 0)`
///
/// ```txt
/// Anchor Point
///   👇
///    0      3
/// 0  P------+
///    | 3x5  |
///    |      |
///    |      |
/// 5  +------+
/// ```
///
/// Let's place it somewhere in the world with transformation:
///
/// - Mirror: {@link Mirror#NONE}
/// - Rotation: {@link Rotation#CLOCKWISE_90}
///
/// ```txt
///    4          9
/// 2  +----------P 👈Anchor point
///    | 5x3      |
///    |          |
/// 5  +----------+
/// ```
///
/// - BoundingBox: `[(4, 2), (9, 5)]`
///
public final class Parcel {
  public static final Codec<Parcel> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Parcel::uuid),
                      Codec.STRING.optionalFieldOf("dimension").forGetter(Parcel::dimension),
                      ParcelMeta.CODEC.fieldOf("meta").forGetter(Parcel::meta),
                      ParcelTransform.CODEC.fieldOf("transform").forGetter(Parcel::transform),
                      Visual.CODEC.fieldOf("visual").forGetter(Parcel::visual),
                      ParcelPermissions.CONFIG_CODEC
                          .fieldOf("permissions")
                          .forGetter(Parcel::permissions),
                      ArchiveSync.CODEC
                          .optionalFieldOf("archive_sync")
                          .forGetter(Parcel::archiveSync))
                  .apply(inst, Parcel::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;
  private @Nullable String dimension;
  private final ParcelMeta meta;
  private ParcelTransform transform;
  private Visual visual;
  private PermissionConfig<ParcelPermissions> permissions;
  private @Nullable ArchiveSync archiveSync;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Parcel(
      UUID uuid,
      Optional<String> dimension,
      ParcelMeta meta,
      ParcelTransform transform,
      Visual visual,
      PermissionConfig<ParcelPermissions> permissions,
      Optional<ArchiveSync> archiveSync) {
    this.uuid = uuid;
    this.dimension = dimension.orElse(null);
    this.meta = meta;
    this.transform = transform;
    this.visual = visual;
    this.permissions = permissions;
    this.archiveSync = archiveSync.orElse(null);
  }

  // ////////////////////////////////////////////////////////////////
  // Serialized Field Getters
  // ////////////////////////////////////////////////////////////////

  public UUID uuid() {
    return uuid;
  }

  /** Dimension owning this runtime parcel registration. */
  public Optional<String> dimension() {
    return Optional.ofNullable(dimension);
  }

  /** Assigns the owning dimension; moving across dimensions requires a distinct use case. */
  public void assignDimension(String dimension) {
    if (dimension == null || dimension.isBlank()) {
      throw new IllegalArgumentException("Parcel dimension must not be blank");
    }
    if (this.dimension != null && !this.dimension.equals(dimension)) {
      throw new IllegalStateException("Parcel already belongs to dimension " + this.dimension);
    }
    this.dimension = dimension;
  }

  public ParcelMeta meta() {
    return meta;
  }

  public ParcelTransform transform() {
    return transform;
  }

  public Visual visual() {
    return visual;
  }

  public PermissionConfig<ParcelPermissions> permissions() {
    return permissions;
  }

  /**
   * Archive metadata cached when this parcel last synced with its archive. Absent until the first
   * save, load, or import. The cache lets clients compare the registered bounds against the synced
   * bounds without opening the repository; it is not a content-dirty query.
   */
  public Optional<ArchiveSync> archiveSync() {
    return Optional.ofNullable(archiveSync);
  }

  /** Updates the cached archive metadata after a successful sync with the archive. */
  public void setArchiveSync(@Nullable ArchiveSync archiveSync) {
    this.archiveSync = archiveSync;
  }

  // ////////////////////////////////////////////////////////////////
  // Others
  // ////////////////////////////////////////////////////////////////

  public BoundingBox getBoundingBox() {
    var anchor = meta.anchor();
    var size = meta.size();
    BlockPos minPos = transform.apply(new BlockPos(-anchor.getX(), -anchor.getY(), -anchor.getZ()));
    BlockPos maxPos =
        transform.apply(
            new BlockPos(
                size.getX() - 1 - anchor.getX(),
                size.getY() - 1 - anchor.getY(),
                size.getZ() - 1 - anchor.getZ()));
    return BoundingBox.fromCorners(minPos, maxPos);
  }

  public Vec3i getSizeWorldSpace() {
    return transform.applyToSize(meta.size());
  }

  public Vec3i getSizeParcelSpace() {
    return meta.size();
  }

  /**
   * The anchor's absolute world position, which is also the placement translation. It is a
   * persistent reference point: adjusting the parcel bounds never changes it, and moving it is an
   * explicit placement change.
   */
  public Vec3i anchorPos() {
    return transform.translation();
  }

  /** Creates a parcel model with a new UUID and default visual, permission, and storage settings. */
  public static Parcel create(ParcelMeta meta, ParcelTransform transform) {
    return create(meta, transform, new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  /** Creates a parcel model with a new UUID and the supplied permission requirements. */
  public static Parcel create(
      ParcelMeta meta,
      ParcelTransform transform,
      PermissionConfig<ParcelPermissions> permissions) {
    return new Parcel(
        UUID.randomUUID(),
        Optional.empty(),
        meta,
        transform,
        new Visual(),
        permissions,
        Optional.empty());
  }

  /**
   * Archive metadata cached on the parcel at its last sync (save, load, or import).
   *
   * @param size Content size of the synced snapshot, in parcel-local coordinates.
   * @param anchor Anchor offset of the synced snapshot, in parcel-local coordinates.
   * @param repositorySizeBytes On-disk size of the archive repository in bytes.
   */
  public record ArchiveSync(Vec3i size, Vec3i anchor, long repositorySizeBytes) {
    public static final Codec<ArchiveSync> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Vec3i.CODEC.fieldOf("size").forGetter(ArchiveSync::size),
                        Vec3i.CODEC.fieldOf("anchor").forGetter(ArchiveSync::anchor),
                        Codec.LONG.fieldOf("repository_size_bytes")
                            .forGetter(ArchiveSync::repositorySizeBytes))
                    .apply(inst, ArchiveSync::new));
  }

  /**
   * Returns the anchor position for a freshly created parcel placed over the given world box.
   *
   * <p>New parcels start with the anchor at the local minimum corner, so this is the
   * orientation-dependent corner of the box. Once created, the anchor stays at its absolute world
   * position even if the bounds are adjusted later.
   */
  public static Vec3i anchorPos(Mirror mirror, Rotation rotation, BoundingBox box) {
    return switch (mirror) {
      // A+
      // ++
      case NONE ->
          switch (rotation) {
            case NONE -> new Vec3i(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new Vec3i(1 + box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new Vec3i(1 + box.maxX(), box.minY(), 1 + box.maxZ());
            case COUNTERCLOCKWISE_90 -> new Vec3i(box.minX(), box.minY(), 1 + box.maxZ());
          };
      // ++
      // A+
      case LEFT_RIGHT ->
          switch (rotation) {
            case NONE -> new Vec3i(box.minX(), box.minY(), 1 + box.maxZ());
            case CLOCKWISE_90 -> new Vec3i(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new Vec3i(1 + box.maxX(), box.minY(), box.minZ());
            case COUNTERCLOCKWISE_90 -> new Vec3i(1 + box.maxX(), box.minY(), 1 + box.maxZ());
          };
      // +A
      // ++
      case FRONT_BACK ->
          switch (rotation) {
            case NONE -> new Vec3i(1 + box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new Vec3i(1 + box.maxX(), box.minY(), 1 + box.maxZ());
            case CLOCKWISE_180 -> new Vec3i(box.minX(), box.minY(), 1 + box.maxZ());
            case COUNTERCLOCKWISE_90 -> new Vec3i(box.minX(), box.minY(), box.minZ());
          };
    };
  }

  /** Visual settings controlling how a parcel is rendered on the client. */
  public static final class Visual {
    public static final Codec<Visual> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.BOOL.fieldOf("show_wireframe").forGetter(Visual::showWireframe),
                        Codec.BOOL.fieldOf("show_anchor").forGetter(Visual::showAnchor))
                    .apply(inst, Visual::new));

    private boolean showWireframe;
    private boolean showAnchor;

    public Visual() {
      this(true, true);
    }

    private Visual(boolean showWireframe, boolean showAnchor) {
      this.showWireframe = showWireframe;
      this.showAnchor = showAnchor;
    }

    /** Whether the parcel wireframe should be rendered. */
    public boolean showWireframe() {
      return showWireframe;
    }

    /** Sets whether the parcel wireframe should be rendered. */
    public Visual showWireframe(boolean showWireframe) {
      if (this.showWireframe != showWireframe) {
        this.showWireframe = showWireframe;
      }
      return this;
    }

    public boolean showAnchor() {
      return showAnchor;
    }

    public Visual showAnchor(boolean showAnchor) {
      if (this.showAnchor != showAnchor) {
        this.showAnchor = showAnchor;
      }
      return this;
    }
  }

}
