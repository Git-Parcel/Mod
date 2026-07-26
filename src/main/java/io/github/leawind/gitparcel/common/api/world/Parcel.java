package io.github.leawind.gitparcel.common.api.world;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

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
                      ExtraCodecs.JSON
                          .optionalFieldOf("formatConfig")
                          .forGetter(Parcel::formatConfig),
                      ParcelLocation.CODEC.optionalFieldOf("location").forGetter(Parcel::location))
                  .apply(inst, Parcel::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;
  private final ParcelMeta meta;
  private ParcelTransform transform;
  private Visual visual;
  private PermissionConfig<ParcelPermissions> permissions;
  private @Nullable JsonElement formatConfig;

  /**
   * Where to save this parcel.
   *
   * <ul>
   *   <li>If {@code null}, the parcel is saved to the world's internal parcel repository.
   *   <li>If not null, the parcel is saved to custom location in a custom repo.
   * </ul>
   */
  private @Nullable ParcelLocation location;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Parcel(
      UUID uuid,
      ParcelMeta meta,
      ParcelTransform transform,
      Visual visual,
      PermissionConfig<ParcelPermissions> permissions,
      Optional<JsonElement> formatConfig,
      Optional<ParcelLocation> location) {
    this.uuid = uuid;
    this.meta = meta;
    this.transform = transform;
    this.visual = visual;
    this.permissions = permissions;
    this.formatConfig = formatConfig.orElse(null);
    this.location = location.orElse(null);
  }

  // ////////////////////////////////////////////////////////////////
  // Serialized Field Getters
  // ////////////////////////////////////////////////////////////////

  public UUID uuid() {
    return uuid;
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

  public Optional<JsonElement> formatConfig() {
    return Optional.ofNullable(formatConfig);
  }

  public Optional<ParcelLocation> location() {
    return Optional.ofNullable(location);
  }

  /** Changes where future save and Git operations resolve this parcel. */
  public void setLocation(@Nullable ParcelLocation location) {
    this.location = location;
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

  /** Creates a parcel model with a new UUID and default visual, permission, and storage settings. */
  public static Parcel create(ParcelMeta meta, ParcelTransform transform) {
    return create(meta, transform, new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  /** Creates a parcel model with a new UUID and the supplied permission requirements. */
  public static Parcel create(
      ParcelMeta meta,
      ParcelTransform transform,
      PermissionConfig<ParcelPermissions> permissions) {
    return create(meta, transform, permissions, null);
  }

  /** Creates a parcel backed by the supplied storage location. */
  public static Parcel create(
      ParcelMeta meta,
      ParcelTransform transform,
      PermissionConfig<ParcelPermissions> permissions,
      @Nullable ParcelLocation location) {
    return new Parcel(
        UUID.randomUUID(),
        meta,
        transform,
        new Visual(),
        permissions,
        Optional.empty(),
        Optional.ofNullable(location));
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
   * A parcel directory in either a direct repository path or a symbolically named shared
   * repository. Exactly one repository reference is present.
   *
   * @param repo direct Git repository path, or {@code null} for a shared repository
   * @param sharedRepositoryName shared catalog name, or {@code null} for a direct repository
   * @param relative Parcel directory path relative to the repository
   */
  public record ParcelLocation(
      @Nullable Path repo,
      @Nullable String sharedRepositoryName,
      Path relative) {
    private static final Pattern SHARED_REPOSITORY_NAME =
        Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

    public static final Codec<ParcelLocation> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING
                            .optionalFieldOf("repo")
                            .forGetter(ParcelLocation::getRepoPathString),
                        Codec.STRING
                            .optionalFieldOf("shared_repository")
                            .forGetter(ParcelLocation::getSharedRepositoryName),
                        Codec.STRING
                            .fieldOf("relative")
                            .forGetter(ParcelLocation::getParcelPathString))
                    .apply(inst, ParcelLocation::fromSerialized));

    public ParcelLocation {
      if ((repo == null) == (sharedRepositoryName == null)) {
        throw new IllegalArgumentException(
            "Parcel location must have exactly one repository reference");
      }
      if (repo != null) {
        repo = repo.normalize();
      }
      if (sharedRepositoryName != null
          && !SHARED_REPOSITORY_NAME.matcher(sharedRepositoryName).matches()) {
        throw new IllegalArgumentException("Invalid shared repository name");
      }
      relative = Objects.requireNonNull(relative, "relative").normalize();
      if (relative.isAbsolute()) {
        throw new IllegalArgumentException("Parcel path must be relative");
      }
      if (relative.toString().isEmpty()) {
        throw new IllegalArgumentException("Parcel path must not be empty");
      }
      if (relative.startsWith("..")) {
        throw new IllegalArgumentException("Parcel path must stay inside the repository");
      }
      for (Path part : relative) {
        if (part.toString().equals(".git")) {
          throw new IllegalArgumentException("Parcel path must not contain .git");
        }
      }
      if (sharedRepositoryName != null
          && relative.getName(0).toString().equals("meta.json")) {
        throw new IllegalArgumentException(
            "Shared parcel path conflicts with repository metadata");
      }
    }

    public ParcelLocation(Path repo, Path relative) {
      this(repo, null, relative);
    }

    public ParcelLocation(String repoPathString, String parcelPathString) {
      this(Path.of(repoPathString), Path.of(parcelPathString));
    }

    public static ParcelLocation shared(String repositoryName, Path relative) {
      return new ParcelLocation(null, repositoryName, relative);
    }

    public Optional<String> sharedRepository() {
      return Optional.ofNullable(sharedRepositoryName);
    }

    public boolean isShared() {
      return sharedRepositoryName != null;
    }

    private static ParcelLocation fromSerialized(
        Optional<String> repo,
        Optional<String> sharedRepository,
        String relative) {
      return new ParcelLocation(
          repo.map(Path::of).orElse(null),
          sharedRepository.orElse(null),
          Path.of(relative));
    }

    private Optional<String> getRepoPathString() {
      return Optional.ofNullable(repo).map(Path::toString);
    }

    private Optional<String> getSharedRepositoryName() {
      return Optional.ofNullable(sharedRepositoryName);
    }

    private String getParcelPathString() {
      return relative.toString();
    }

    public Path getParcelPath() {
      if (repo == null) {
        throw new IllegalStateException(
            "Shared parcel locations require a shared repository root");
      }
      return repo.resolve(relative).normalize();
    }
  }
}
