package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.permission.ParcelPermissions;
import io.github.leawind.gitparcel.utils.permission.PermissionConfig;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/** Parcel represents a cuboid area in the world. */
public class Parcel {

  public static final Codec<Parcel> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Parcel::uuid),
                      BoundingBox.CODEC.fieldOf("bounding_box").forGetter(Parcel::boundingBox),
                      Mirror.CODEC.fieldOf("mirror").forGetter(Parcel::mirror),
                      Rotation.CODEC.fieldOf("rotation").forGetter(Parcel::rotation),
                      Visual.CODEC.fieldOf("visual").forGetter(Parcel::visual),
                      ParcelPermissions.CONFIG_CODEC
                          .fieldOf("permissions")
                          .forGetter(Parcel::permissions))
                  .apply(inst, Parcel::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;

  private BoundingBox boundingBox;
  private Mirror mirror;
  private Rotation rotation;

  private PermissionConfig<ParcelPermissions> permissions;

  private Visual visual;

  // ////////////////////////////////////////////////////////////////
  // Unserialized Fields
  // ////////////////////////////////////////////////////////////////

  private @Nullable GitParcelLevelSavedData levelSavedData;

  public Parcel(UUID uuid, BoundingBox boundingBox, Visual visual) {
    this(
        uuid,
        boundingBox,
        Mirror.NONE,
        Rotation.NONE,
        visual,
        new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  public Parcel(
      UUID uuid,
      BoundingBox boundingBox,
      Mirror mirror,
      Rotation rotation,
      Visual visual,
      PermissionConfig<ParcelPermissions> permissions) {
    this.uuid = uuid;
    this.boundingBox = boundingBox;
    this.mirror = mirror;
    this.rotation = rotation;
    this.visual = visual;
    this.permissions = permissions;
  }

  // ////////////////////////////////////////////////////////////////
  // Serialized Field Getters
  // ////////////////////////////////////////////////////////////////

  public UUID uuid() {
    return uuid;
  }

  public BoundingBox boundingBox() {
    return boundingBox;
  }

  public Mirror mirror() {
    return mirror;
  }

  public Rotation rotation() {
    return rotation;
  }

  public Visual visual() {
    return visual;
  }

  public PermissionConfig<ParcelPermissions> permissions() {
    return permissions;
  }

  // ////////////////////////////////////////////////////////////////
  // Others
  // ////////////////////////////////////////////////////////////////

  public ParcelTransform getTransform() {
    return new ParcelTransform(mirror, rotation, getPivotBlock());
  }

  public Vec3i getSizeWorldSpace() {
    return new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
  }

  public Vec3i getSizeParcelSpace() {
    return ParcelTransform.rotateSizeInverted(rotation, getSizeWorldSpace());
  }

  /** Get pivot block position in world space */
  public BlockPos getPivotBlock() {
    return ParcelTransform.getPivotPos(mirror, rotation, boundingBox);
  }

  public boolean hasOrientation() {
    return mirror != Mirror.NONE || rotation != Rotation.NONE;
  }

  void setLevelSavedData(@Nullable GitParcelLevelSavedData levelSavedData) {
    this.levelSavedData = levelSavedData;
  }

  public @Nullable GitParcelLevelSavedData getLevelSavedData() {
    return levelSavedData;
  }

  public void setDirty(boolean dirty) {
    if (levelSavedData != null) {
      levelSavedData.setDirty(dirty);
    }
  }

  /** Visual settings controlling how a parcel is rendered on the client. */
  public static final class Visual {
    public static final Codec<Visual> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(Codec.BOOL.fieldOf("show_wireframe").forGetter(Visual::showWireframe))
                    .apply(inst, Visual::new));

    private boolean showWireframe;

    public Visual(boolean showWireframe) {
      this.showWireframe = showWireframe;
    }

    /** Whether the parcel wireframe should be rendered. */
    public boolean showWireframe() {
      return showWireframe;
    }

    /** Sets whether the parcel wireframe should be rendered. */
    public void showWireframe(boolean showWireframe) {
      this.showWireframe = showWireframe;
    }
  }
}
