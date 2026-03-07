package io.github.leawind.gitparcel.api.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
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
 * <p>Provides methods to apply transformations to {@link Vec3i}, {@link BlockPos}, {@link
 * BlockState}, and {@link Vec3}. Inverted transformations are supported.
 *
 * <p>The pivot point for mirroring and rotation is (0, 0, 0).
 *
 * @see StructurePlaceSettings
 */
public record ParcelTransform(Mirror mirror, Rotation rotation, Vec3i translation) {
  /** Identity transform. */
  public static final ParcelTransform IDENTITY = new ParcelTransform(Vec3i.ZERO);

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
    var rotated = applyRotation(size);
    return new Vec3i(Math.abs(rotated.getX()), rotated.getY(), Math.abs(rotated.getZ()));
  }

  /**
   * Transforms a world space size vector using the inverted transformation.
   *
   * <p>Only inverted rotation is considered (Y-axis only). X and Z components are normalized to
   * absolute values.
   *
   * @param size The world space size vector
   * @return The transformed size vector
   */
  public Vec3i applyToSizeInverted(Vec3i size) {
    var rotated = applyRotationInverted(size);
    return new Vec3i(Math.abs(rotated.getX()), rotated.getY(), Math.abs(rotated.getZ()));
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
   * Applies the mirror transformation to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The mirrored vector
   */
  public Vec3i applyMirror(Vec3i vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3i(-vec.getX(), vec.getY(), vec.getZ());
      case FRONT_BACK -> new Vec3i(vec.getX(), vec.getY(), -vec.getZ());
    };
  }

  /**
   * Applies the mirror transformation to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The mirrored vector
   */
  private Vec3 applyMirror(Vec3 vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3(-vec.x, vec.y, vec.z);
      case FRONT_BACK -> new Vec3(vec.x, vec.y, -vec.z);
    };
  }

  /**
   * Applies the mirror transformation to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The mirrored position
   */
  public BlockPos applyMirror(BlockPos pos) {
    return switch (mirror) {
      case NONE -> pos;
      case LEFT_RIGHT -> new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
      case FRONT_BACK -> new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
    };
  }

  /**
   * Applies the mirror transformation to a {@link BlockState}.
   *
   * @param blockState The block state
   * @return The mirrored block state
   */
  public BlockState applyMirror(BlockState blockState) {
    return blockState.mirror(mirror);
  }

  /**
   * Applies the rotation transformation to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The rotated vector
   */
  public Vec3i applyRotation(Vec3i vec) {
    return rotate(rotation, vec);
  }

  /**
   * Applies the rotation transformation to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The rotated vector
   */
  public Vec3 applyRotation(Vec3 vec) {
    return rotate(rotation, vec);
  }

  /**
   * Applies the rotation transformation to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The rotated position
   */
  public BlockPos applyRotation(BlockPos pos) {
    return pos.rotate(rotation);
  }

  /**
   * Applies the rotation transformation to a {@link BlockState}.
   *
   * @param blockState The block state
   * @return The rotated block state
   */
  public BlockState applyRotation(BlockState blockState) {
    return blockState.rotate(rotation);
  }

  /**
   * Applies the inverted rotation transformation to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The inversely rotated vector
   */
  public Vec3i applyRotationInverted(Vec3i vec) {
    return rotate(invert(rotation), vec);
  }

  /**
   * Applies the inverted rotation transformation to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The inversely rotated vector
   */
  private Vec3 applyRotationInverted(Vec3 vec) {
    return rotate(invert(rotation), vec);
  }

  /**
   * Applies the inverted rotation transformation to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The inversely rotated position
   */
  public BlockPos applyRotationInverted(BlockPos pos) {
    return pos.rotate(invert(rotation));
  }

  /**
   * Applies the inverted rotation transformation to a {@link BlockState}.
   *
   * @param blockState The block state
   * @return The inversely rotated block state
   */
  public BlockState applyRotationInverted(BlockState blockState) {
    return blockState.rotate(invert(rotation));
  }

  /**
   * Applies the translation transformation to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The translated vector
   */
  public Vec3i applyTranslation(Vec3i vec) {
    return vec.offset(translation);
  }

  /**
   * Applies the translation transformation to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The translated position
   */
  public BlockPos applyTranslation(BlockPos pos) {
    return pos.offset(translation);
  }

  /**
   * Applies the inverted translation transformation to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The inversely translated vector
   */
  public Vec3i applyTranslationInverted(Vec3i vec) {
    return vec.subtract(translation);
  }

  /**
   * Applies the inverted translation transformation to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The inversely translated vector
   */
  private Vec3 applyTranslationInverted(Vec3 vec) {
    return vec.subtract(translation.getX(), translation.getY(), translation.getZ());
  }

  /**
   * Applies the inverted translation transformation to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The inversely translated position
   */
  public BlockPos applyTranslationInverted(BlockPos pos) {
    return pos.subtract(translation);
  }

  /**
   * Applies all transformations (mirror, rotate, translate) to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The transformed vector
   */
  public Vec3i apply(Vec3i vec) {
    vec = applyMirror(vec);
    vec = applyRotation(vec);
    vec = applyTranslation(vec);
    return vec;
  }

  /**
   * Applies all transformations (mirror, rotate, translate) to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The transformed position
   */
  public BlockPos apply(BlockPos pos) {
    pos = applyMirror(pos);
    pos = applyRotation(pos);
    pos = applyTranslation(pos);
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

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link Vec3i}.
   *
   * @param vec The vector
   * @return The inversely transformed vector
   */
  public Vec3i applyInverted(Vec3i vec) {
    vec = applyTranslationInverted(vec);
    vec = applyRotationInverted(vec);
    vec = applyMirror(vec);
    return vec;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link Vec3}.
   *
   * @param vec The vector
   * @return The inversely transformed vector
   */
  public Vec3 applyInverted(Vec3 vec) {
    vec = applyTranslationInverted(vec);
    vec = applyRotationInverted(vec);
    vec = applyMirror(vec);
    return vec;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a {@link BlockPos}.
   *
   * @param pos The position
   * @return The inversely transformed position
   */
  public BlockPos applyInverted(BlockPos pos) {
    pos = applyTranslationInverted(pos);
    pos = applyRotationInverted(pos);
    pos = applyMirror(pos);
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
    return blockState.rotate(invert(rotation)).mirror(mirror);
  }

  /**
   * Rotates a {@link Vec3i} by the specified rotation.
   *
   * @param rotation The rotation
   * @param vec The vector
   * @return The rotated vector
   */
  private static Vec3i rotate(Rotation rotation, Vec3i vec) {
    return switch (rotation) {
      case NONE -> vec;
      case CLOCKWISE_90 -> new Vec3i(-vec.getZ(), vec.getY(), vec.getX());
      case CLOCKWISE_180 -> new Vec3i(-vec.getX(), vec.getY(), -vec.getZ());
      case COUNTERCLOCKWISE_90 -> new Vec3i(vec.getZ(), vec.getY(), -vec.getX());
    };
  }

  /**
   * Rotates a {@link Vec3} by the specified rotation.
   *
   * @param rotation The rotation
   * @param vec The vector
   * @return The rotated vector
   */
  private static Vec3 rotate(Rotation rotation, Vec3 vec) {
    return switch (rotation) {
      case NONE -> vec;
      case CLOCKWISE_90 -> new Vec3(-vec.z, vec.y, vec.x);
      case CLOCKWISE_180 -> new Vec3(-vec.x, vec.y, -vec.z);
      case COUNTERCLOCKWISE_90 -> new Vec3(vec.z, vec.y, -vec.x);
    };
  }

  /**
   * Inverts the specified rotation.
   *
   * @param rotation The rotation
   * @return The inverted rotation
   */
  private static Rotation invert(Rotation rotation) {
    return switch (rotation) {
      case NONE, CLOCKWISE_180 -> rotation;
      case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
      case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
    };
  }
}
