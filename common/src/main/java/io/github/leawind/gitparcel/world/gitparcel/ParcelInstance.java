package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Parcel Instance represents a specific instance of a parcel in the world.
 *
 * <p>It's saved and loaded with the world level
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
                          .forGetter(ParcelInstance::showBoundingBox))
                  .apply(inst, ParcelInstance::new));

  private final UUID uuid;
  private BoundingBox boundingBox;

  private Mirror mirror = Mirror.NONE;
  private Rotation rotation = Rotation.NONE;

  private boolean showBoundingBox = true;

  public ParcelInstance() {
    this(UUID.randomUUID());
  }

  public ParcelInstance(UUID uuid) {
    this.uuid = uuid;
  }

  public ParcelInstance(
      UUID uuid,
      BoundingBox boundingBox,
      Mirror mirror,
      Rotation rotation,
      Boolean showBoundingBox) {
    this.uuid = uuid;
    this.boundingBox = boundingBox;
    this.mirror = mirror;
    this.rotation = rotation;
    this.showBoundingBox = showBoundingBox;
  }

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
}
