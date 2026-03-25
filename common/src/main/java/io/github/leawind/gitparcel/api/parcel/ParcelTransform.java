package io.github.leawind.gitparcel.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.utils.TransformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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
 * <p>Provides methods to apply transformations to {@link Vec3i}, {@link BlockPos}, {@link
 * BlockState}, and {@link Vec3}. Inverted transformations are supported.
 *
 * <p>The pivot point for mirroring and rotation is (0, 0, 0).
 *
 * @see StructurePlaceSettings
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
   * Creates a transform with only translation.
   *
   * @param translation The translation offset
   */
  public ParcelTransform(Vec3i translation) {
    this(Mirror.NONE, Rotation.NONE, translation);
  }

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

  /**
   * Applies all transformations (mirror, rotate, translate) to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The transformed vector
   */
  public Vec3i apply(Vec3i vec) {
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

  /**
   * Applies mirror and rotation transformations to a {@link BlockState}.
   *
   * <p>Translation does not affect {@link BlockState}.
   *
   * @param blockState The block state
   * @return The transformed block state
   */
  public BlockState apply(BlockState blockState) {
    return blockState.mirror(mirror).rotate(rotation);
  }

  public void apply(Matrix4f matrix) {
    TransformUtils.mirror(mirror, matrix);
    TransformUtils.rotateY(rotation, matrix);
    TransformUtils.translate(translation, matrix);
  }

  public BoundingBox apply(BoundingBox boundingBox) {
    boundingBox = TransformUtils.mirror(mirror, boundingBox);
    boundingBox = TransformUtils.rotate(rotation, boundingBox);
    boundingBox = TransformUtils.translate(translation, boundingBox);
    return boundingBox;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The inversely transformed vector
   */
  public Vec3i applyInverted(Vec3i vec) {
    vec = TransformUtils.translateInverted(translation, vec);
    vec = TransformUtils.rotateInverted(rotation, vec);
    vec = TransformUtils.mirror(mirror, vec);
    return vec;
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

  /**
   * Applies the inverted transformations (mirror, rotate) to a {@link BlockState}.
   *
   * <p>Translation does not affect {@link BlockState}.
   *
   * @param blockState The block state
   * @return The inversely transformed block state
   */
  public BlockState applyInverted(BlockState blockState) {
    return blockState.rotate(TransformUtils.invert(rotation)).mirror(mirror);
  }

  public static Vec3i rotateSize(Rotation rotation, Vec3i size) {
    return switch (rotation) {
      case NONE, CLOCKWISE_180 -> size;
      case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(size.getZ(), size.getY(), size.getX());
    };
  }

  public static BlockPos getPivotPos(Mirror mirror, Rotation rotation, BoundingBox bounds) {
    return switch (mirror) {
      case NONE ->
          switch (rotation) {
            case NONE -> new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
            case CLOCKWISE_90 -> new BlockPos(bounds.maxX(), bounds.minY(), bounds.minZ());
            case CLOCKWISE_180 -> new BlockPos(bounds.maxX(), bounds.minY(), bounds.maxZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(bounds.minX(), bounds.minY(), bounds.maxZ());
          };
      case LEFT_RIGHT ->
          switch (rotation) {
            case NONE -> new BlockPos(bounds.minX(), bounds.minY(), bounds.maxZ());
            case CLOCKWISE_90 -> new BlockPos(bounds.maxX(), bounds.minY(), -bounds.minZ());
            case CLOCKWISE_180 -> new BlockPos(bounds.maxX(), bounds.minY(), -bounds.maxZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(bounds.minX(), bounds.minY(), -bounds.maxZ());
          };
      case FRONT_BACK ->
          switch (rotation) {
            case NONE -> new BlockPos(bounds.maxX(), bounds.minY(), bounds.minZ());
            case CLOCKWISE_90 -> new BlockPos(-bounds.maxX(), bounds.minY(), bounds.minZ());
            case CLOCKWISE_180 -> new BlockPos(-bounds.maxX(), bounds.minY(), bounds.maxZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(-bounds.minX(), bounds.minY(), bounds.maxZ());
          };
    };
  }
}
