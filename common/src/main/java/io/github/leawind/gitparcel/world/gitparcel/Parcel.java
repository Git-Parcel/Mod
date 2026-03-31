package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.permission.ParcelPermissions;
import io.github.leawind.gitparcel.repo.CustomParcelInRepo;
import io.github.leawind.gitparcel.repo.InternalParcelInRepo;
import io.github.leawind.gitparcel.repo.ParcelInRepo;
import io.github.leawind.gitparcel.storage.StorageManager;
import io.github.leawind.gitparcel.storage.WorldStorageManager;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/// Parcel represents an axially aligned cuboid area in the world.
///
/// - The pivot and anchor are treated as a point, not block position.
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
/// - Pivot point is `(0, 0)`
///
/// ```txt
/// Pivot Point
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
/// 2  +----------P 👈Pivot point
///    | 5x3      |
///    |          |
/// 5  +----------+
/// ```
///
/// - BoundingBox: `[(4, 2), (9, 5)]`
///
public final class Parcel {
  public static final Logger LOGGER = LogUtils.getLogger();

  public static final Codec<Parcel> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Parcel::uuid),
                      ParcelMeta.CODEC.fieldOf("meta").forGetter(Parcel::meta),
                      ParcelTransform.CODEC.fieldOf("transform").forGetter(Parcel::transform),
                      Visual.CODEC.fieldOf("visual").forGetter(Parcel::visual),
                      ParcelPermissions.CONFIG_CODEC
                          .fieldOf("permissions")
                          .forGetter(Parcel::permissions),
                      ParcelLocation.CODEC
                          .optionalFieldOf("location")
                          .forGetter(Parcel::optionalLocation))
                  .apply(inst, Parcel::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;
  private final ParcelMeta meta;
  private ParcelTransform transform;

  /**
   * Where to save this parcel.
   *
   * <ul>
   *   <li>If {@code null}, the parcel is saved to internal parcel repo, refer to {@link
   *       WorldStorageManager#getInternalParcelsDir}.
   *   <li>If not null, the parcel is saved to custom location in a custom repo.
   * </ul>
   */
  private @Nullable ParcelLocation location;

  private PermissionConfig<ParcelPermissions> permissions;

  private Visual visual;

  // ////////////////////////////////////////////////////////////////
  // Unserialized Fields
  // ////////////////////////////////////////////////////////////////

  /** Set by Level Saved Data, when creating a new one or loaded from saved data */
  private @Nullable GitParcelLevelSavedData levelSavedData;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Parcel(
      UUID uuid,
      ParcelMeta meta,
      ParcelTransform transform,
      Visual visual,
      PermissionConfig<ParcelPermissions> permissions,
      Optional<ParcelLocation> location) {
    this.uuid = uuid;
    this.meta = meta;
    this.transform = transform;
    this.visual = visual;
    this.permissions = permissions;
    this.location = location.orElse(null);
  }

  // ////////////////////////////////////////////////////////////////
  // Serialized Field Getters
  // ////////////////////////////////////////////////////////////////

  public UUID uuid() {
    return uuid;
  }

  public ParcelTransform transform() {
    return transform;
  }

  private Optional<ParcelLocation> optionalLocation() {
    return Optional.ofNullable(location);
  }

  public Visual visual() {
    return visual;
  }

  public PermissionConfig<ParcelPermissions> permissions() {
    return permissions;
  }

  public ParcelMeta meta() {
    return meta;
  }

  // ////////////////////////////////////////////////////////////////
  // Others
  // ////////////////////////////////////////////////////////////////

  public BoundingBox getBoundingBox() {
    var localSize = meta.size();
    var maxBlockPos =
        new BlockPos(localSize.getX() - 1, localSize.getY() - 1, localSize.getZ() - 1);
    return BoundingBox.fromCorners(getPivotBlockPos(), transform.apply(maxBlockPos));
  }

  public Vec3i getSizeWorldSpace() {
    return transform.applyToSize(meta.size());
  }

  public Vec3i getSizeParcelSpace() {
    return meta.size();
  }

  public Vec3 getPivot() {
    var translation = transform.translation();
    return new Vec3(translation.getX(), translation.getY(), translation.getZ());
  }

  public Vec3 getPivotBlockCenter() {
    return transform.apply(new Vec3(0.5, 0.5, 0.5));
  }

  /** Get pivot block position in world space */
  public BlockPos getPivotBlockPos() {
    return BlockPos.containing(getPivotBlockCenter());
  }

  public void setLevelSavedData(@Nullable GitParcelLevelSavedData levelSavedData) {
    this.levelSavedData = levelSavedData;
  }

  public @Nullable GitParcelLevelSavedData getLevelSavedData() {
    return levelSavedData;
  }

  /**
   * @return ServerLevel of this parcel or null if not initialized
   */
  public @Nullable ServerLevel getLevel() {
    return levelSavedData == null ? null : levelSavedData.getLevel();
  }

  /**
   * @throws NullPointerException if this parcel is manually created and levelSavedData is not set
   */
  public ParcelInRepo getParcelInRepo() throws NullPointerException {
    if (location == null) {
      var storage = StorageManager.getInstance((Objects.requireNonNull(getLevel())).getServer());
      var repoPath = storage.worldStorage().getInternalParcelsDir().resolve(uuid.toString());
      return new InternalParcelInRepo(repoPath);
    } else {
      return new CustomParcelInRepo(location.repo, location.relative);
    }
  }

  public <C extends ParcelFormatConfig<C>> void save(boolean ignoreEntities)
      throws IOException, ParcelException {
    ParcelFormat.save(
        getLevel(), transform, meta, getParcelInRepo().getParcelDir(), ignoreEntities);
  }

  /** Should be called when this parcel is updated. */
  public void emitUpdate() {
    if (levelSavedData == null) {
      GitParcelMod.LOGGER.warn("Parcel {} is not in a level saved data", this);
      return;
    }
    levelSavedData.setDirty();
    levelSavedData.emitParcelUpdate(this);
  }

  public static Parcel create(BoundingBox boundingBox, Mirror mirror, Rotation rotation) {

    var pivot = getPivot(mirror, rotation, boundingBox);
    ParcelTransform transform =
        new ParcelTransform(
            mirror, rotation, new Vec3i((int) pivot.x, (int) pivot.y, (int) pivot.z));

    var meta =
        ParcelMeta.from(ParcelFormatRegistry.INSTANCE.defaultSaver().info(), boundingBox, rotation);

    return new Parcel(
        UUID.randomUUID(),
        meta,
        transform,
        new Visual(),
        new PermissionConfig<>(ParcelPermissions.REGISTRY),
        Optional.empty());
  }

  public static BlockPos getPivotBlockPos(Mirror mirror, Rotation rotation, BoundingBox box) {
    return switch (mirror) {
      // P+
      // ++
      case NONE ->
          switch (rotation) {
            case NONE -> new BlockPos(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new BlockPos(box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new BlockPos(box.maxX(), box.minY(), box.maxZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(box.minX(), box.minY(), box.maxZ());
          };
      // ++
      // P+
      case LEFT_RIGHT ->
          switch (rotation) {
            case NONE -> new BlockPos(box.minX(), box.minY(), box.maxZ());
            case CLOCKWISE_90 -> new BlockPos(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new BlockPos(box.maxX(), box.minY(), box.minZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(box.maxX(), box.minY(), box.maxZ());
          };
      // +P
      // ++
      case FRONT_BACK ->
          switch (rotation) {
            case NONE -> new BlockPos(box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new BlockPos(box.maxX(), box.minY(), box.maxZ());
            case CLOCKWISE_180 -> new BlockPos(box.minX(), box.minY(), box.maxZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(box.minX(), box.minY(), box.minZ());
          };
    };
  }

  public static Vec3 getPivot(Mirror mirror, Rotation rotation, BoundingBox box) {
    return switch (mirror) {
      // P+
      // ++
      case NONE ->
          switch (rotation) {
            case NONE -> new Vec3(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new Vec3(1 + box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new Vec3(1 + box.maxX(), box.minY(), 1 + box.maxZ());
            case COUNTERCLOCKWISE_90 -> new Vec3(box.minX(), box.minY(), 1 + box.maxZ());
          };
      // ++
      // P+
      case LEFT_RIGHT ->
          switch (rotation) {
            case NONE -> new Vec3(box.minX(), box.minY(), 1 + box.maxZ());
            case CLOCKWISE_90 -> new Vec3(box.minX(), box.minY(), box.minZ());
            case CLOCKWISE_180 -> new Vec3(1 + box.maxX(), box.minY(), box.minZ());
            case COUNTERCLOCKWISE_90 -> new Vec3(1 + box.maxX(), box.minY(), 1 + box.maxZ());
          };
      // +P
      // ++
      case FRONT_BACK ->
          switch (rotation) {
            case NONE -> new Vec3(1 + box.maxX(), box.minY(), box.minZ());
            case CLOCKWISE_90 -> new Vec3(1 + box.maxX(), box.minY(), 1 + box.maxZ());
            case CLOCKWISE_180 -> new Vec3(box.minX(), box.minY(), 1 + box.maxZ());
            case COUNTERCLOCKWISE_90 -> new Vec3(box.minX(), box.minY(), box.minZ());
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

  /**
   * @param repo Git repository path
   * @param relative Parcel directory path relative to the repo path
   */
  public record ParcelLocation(Path repo, Path relative) {
    public static final Codec<ParcelLocation> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.fieldOf("repo").forGetter(ParcelLocation::getRepoPathString),
                        Codec.STRING
                            .fieldOf("relative")
                            .forGetter(ParcelLocation::getParcelPathString))
                    .apply(inst, ParcelLocation::new));

    public ParcelLocation(String repoPathString, String parcelPathString) {
      this(Path.of(repoPathString), Path.of(parcelPathString));
      if (relative.isAbsolute()) {
        throw new IllegalArgumentException("Parcel path must be relative");
      }
    }

    private String getRepoPathString() {
      return repo.toString();
    }

    private String getParcelPathString() {
      return relative.toString();
    }

    public Path getParcelPath() {
      return repo.resolve(relative);
    }
  }
}
