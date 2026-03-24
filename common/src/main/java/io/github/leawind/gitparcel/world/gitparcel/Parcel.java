package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
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
                          .forGetter(Parcel::permissions))
                  .apply(inst, Parcel::new));

  // ////////////////////////////////////////////////////////////////
  // Serialized Fields
  // ////////////////////////////////////////////////////////////////

  private final UUID uuid;
  private final ParcelMeta meta;
  private ParcelTransform transform;

  private PermissionConfig<ParcelPermissions> permissions;

  private Visual visual;

  // ////////////////////////////////////////////////////////////////
  // Unserialized Fields
  // ////////////////////////////////////////////////////////////////

  private @Nullable GitParcelLevelSavedData levelSavedData;

  public static Parcel from(UUID uuid, BoundingBox boundingBox, Visual visual) {
    var pivot = ParcelTransform.getPivotPos(Mirror.NONE, Rotation.NONE, boundingBox);
    ParcelTransform transform = new ParcelTransform(pivot);

    var meta =
        ParcelMeta.from(
            ParcelFormatRegistry.INSTANCE.defaultSaver().info(), boundingBox, Rotation.NONE);

    return new Parcel(
        uuid, meta, transform, visual, new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  public Parcel(
      UUID uuid,
      ParcelMeta meta,
      ParcelTransform transform,
      Visual visual,
      PermissionConfig<ParcelPermissions> permissions) {
    this.uuid = uuid;
    this.meta = meta;
    this.transform = transform;
    this.visual = visual;
    this.permissions = permissions;
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
    var localBox =
        new BoundingBox(0, 0, 0, localSize.getX() - 1, localSize.getY() - 1, localSize.getZ() - 1);
    return transform.apply(localBox);
  }

  public Vec3i getSizeWorldSpace() {
    return transform.applyToSize(meta.size());
  }

  public Vec3i getSizeParcelSpace() {
    return meta.size();
  }

  /** Get pivot block position in world space */
  public BlockPos getPivotBlock() {
    return transform.getTranslatedOrigin();
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
