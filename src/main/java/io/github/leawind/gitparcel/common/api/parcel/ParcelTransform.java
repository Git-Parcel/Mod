package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.utils.TransformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a <strong>local to world</strong> transformation applicable to parcels, including
 * mirroring, rotation, and translation.
 *
 * <p>Transformations are applied in the following order:
 *
 * <ol>
 *   <li>Mirror
 *   <li>Rotate
 *   <li>Translate
 * </ol>
 *
 * <p>The pivot point for mirroring and rotation is (0, 0, 0).
 *
 * @see net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 */
public record ParcelTransform(Mirror mirror, Rotation rotation, Vec3i translation) {
  public static final Codec<ParcelTransform> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Mirror.CODEC.fieldOf("mirror").forGetter(ParcelTransform::mirror),
                      Rotation.CODEC.fieldOf("rotation").forGetter(ParcelTransform::rotation),
                      Vec3i.CODEC.fieldOf("translation").forGetter(ParcelTransform::translation))
                  .apply(inst, ParcelTransform::new));

  /** Identity transform. */
  public static final ParcelTransform IDENTITY =
      new ParcelTransform(Mirror.NONE, Rotation.NONE, Vec3i.ZERO);

  /**
   * Transforms a local space size vector to world space.
   *
   * <p>Mirroring and translation do not affect size. Rotation assumes Minecraft's standard
   * Y-axis-only rotation; X and Z components are normalized to absolute values.
   *
   * @param size The local space size vector
   * @return The world space size vector
   */
  public Vec3i applyToSize(Vec3i size) {
    return rotateSize(rotation, size);
  }

  /**
   * Returns the world origin translated by this transform's offset.
   *
   * @return The translated world origin
   */
  public BlockPos getTranslatedOrigin() {
    return new BlockPos(translation);
  }

  /**
   * Returns {@code true} if this transform includes mirroring or rotation.
   *
   * @return True if mirrored or rotated
   */
  public boolean hasOrientation() {
    return mirror != Mirror.NONE || rotation != Rotation.NONE;
  }

  public Vec3 apply(Vec3 vec) {
    vec = TransformUtils.mirror(mirror, vec);
    vec = TransformUtils.rotate(rotation, vec);
    vec = TransformUtils.translate(translation, vec);
    return vec;
  }

  /**
   * Applies all transformations (mirror, rotate, translate) to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The transformed position
   */
  public BlockPos apply(BlockPos pos) {
    pos = TransformUtils.mirror(mirror, pos);
    pos = TransformUtils.rotate(rotation, pos);
    pos = TransformUtils.translate(translation, pos);
    return pos;
  }

  /** Applies mirror and rotation to a direction vector without translating it. */
  public Vec3 applyVector(Vec3 vec) {
    vec = TransformUtils.mirror(mirror, vec);
    return TransformUtils.rotate(rotation, vec);
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The inversely transformed vector
   */
  public Vec3 applyInverted(Vec3 vec) {
    vec = TransformUtils.translateInverted(translation, vec);
    vec = TransformUtils.rotateInverted(rotation, vec);
    vec = TransformUtils.mirror(mirror, vec);
    return vec;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The inversely transformed position
   */
  public BlockPos applyInverted(BlockPos pos) {
    pos = TransformUtils.translateInverted(translation, pos);
    pos = TransformUtils.rotateInverted(rotation, pos);
    pos = TransformUtils.mirror(mirror, pos);
    return pos;
  }

  /** Applies the inverse rotation and mirror to a direction vector without translating it. */
  public Vec3 applyVectorInverted(Vec3 vec) {
    vec = TransformUtils.rotateInverted(rotation, vec);
    return TransformUtils.mirror(mirror, vec);
  }

  public static Vec3i rotateSize(Rotation rotation, Vec3i size) {
    return switch (rotation) {
      case NONE, CLOCKWISE_180 -> size;
      case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(size.getZ(), size.getY(), size.getX());
    };
  }
}
