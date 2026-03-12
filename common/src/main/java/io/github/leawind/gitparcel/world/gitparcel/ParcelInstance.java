package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.permission.ParcelInstancePermissions;
import io.github.leawind.gitparcel.utils.permission.PermissionSettings;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Parcel Instance represents a specific instance of a parcel in the level.
 *
 * <p>Managed by {@link GitParcelLevelSavedData}
 */
public class ParcelInstance {
  public static final Codec<ParcelInstance> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(ParcelInstance::uuid),
                      BoundingBox.CODEC
                          .fieldOf("bounding_box")
                          .forGetter(ParcelInstance::boundingBox),
                      Mirror.CODEC.fieldOf("mirror").forGetter(ParcelInstance::mirror),
                      Rotation.CODEC.fieldOf("rotation").forGetter(ParcelInstance::rotation),
                      Codec.BOOL
                          .fieldOf("show_bounding_box")
                          .forGetter(ParcelInstance::showBoundingBox),
                      ParcelInstancePermissions.SETTINGS_CODEC
                          .fieldOf("permissions")
                          .forGetter(ParcelInstance::permissions))
                  .apply(inst, ParcelInstance::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;
  private BoundingBox boundingBox;
  private Mirror mirror;
  private Rotation rotation;
  private boolean showBoundingBox;
  private PermissionSettings<ParcelInstancePermissions> permissions;

  // ////////////////////////////////////////////////////////////////
  // Unserialized Fields
  // ////////////////////////////////////////////////////////////////

  private @Nullable GitParcelLevelSavedData levelSavedData;

  public ParcelInstance(UUID uuid, BoundingBox boundingBox, boolean showBoundingBox) {
    this(uuid, boundingBox, Mirror.NONE, Rotation.NONE, showBoundingBox);
  }

  public ParcelInstance(
      UUID uuid,
      BoundingBox boundingBox,
      Mirror mirror,
      Rotation rotation,
      Boolean showBoundingBox) {
    this(
        uuid,
        boundingBox,
        mirror,
        rotation,
        showBoundingBox,
        new PermissionSettings<>(ParcelInstancePermissions.REGISTRY));
  }

  public ParcelInstance(
      UUID uuid,
      BoundingBox boundingBox,
      Mirror mirror,
      Rotation rotation,
      Boolean showBoundingBox,
      PermissionSettings<ParcelInstancePermissions> permissions) {
    this.uuid = uuid;
    this.boundingBox = boundingBox;
    this.mirror = mirror;
    this.rotation = rotation;
    this.showBoundingBox = showBoundingBox;
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

  public boolean showBoundingBox() {
    return showBoundingBox;
  }

  public PermissionSettings<ParcelInstancePermissions> permissions() {
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

  public void setDirty() {
    setDirty(true);
  }

  public void setDirty(boolean dirty) {
    if (levelSavedData != null) {
      levelSavedData.setDirty(dirty);
    }
  }
}
