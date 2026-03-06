package io.github.leawind.gitparcel.api.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a transformation that can be applied to parcels, including mirroring, rotation, and
 * translation.
 *
 * <p>Transformations are applied in the following order:
 *
 * <ol>
 *   <li>Mirror
 *   <li>Rotate
 *   <li>Translate
 * </ol>
 *
 * <p>This class provides methods to apply transformations to various types such as {@link Vec3i},
 * {@link BlockPos}, {@link BlockState}, and {@link Vec3}. It also supports inverted transformations
 * to reverse the effects.
 *
 * <p>The pivot point for mirroring and rotation is (0, 0, 0).
 *
 * @see StructurePlaceSettings
 */
public record ParcelTransform(Mirror mirror, Rotation rotation, Vec3i translation) {
  /** Creates a new ParcelTransform with no transformations (identity transform). */
  public static ParcelTransform none() {
    return new ParcelTransform(Vec3i.ZERO);
  }

  /**
   * Creates a new ParcelTransform with only translation.
   *
   * @param translation The translation offset
   */
  public ParcelTransform(Vec3i translation) {
    this(Mirror.NONE, Rotation.NONE, translation);
  }

  /**
   * Applies the transformation to a size vector, returning the resulting size.
   *
   * <p>Mirroring and translation do not affect size. Rotation is applied assuming Minecraft's
   * standard Y-axis-only rotation; thus, only X and Z components are normalized to absolute values.
   *
   * @param size The original size vector
   * @return The transformed size vector with non-negative X and Z components
   */
  public Vec3i applyToSize(Vec3i size) {
    var rotated = applyRotation(size);
    return new Vec3i(Math.abs(rotated.getX()), rotated.getY(), Math.abs(rotated.getZ()));
  }

  /**
   * Applies the inverted transformation to a size vector, returning the resulting size.
   *
   * <p>Only inverted rotation is considered (Y-axis only). Mirroring and translation do not affect
   * size. X and Z components are normalized to absolute values to ensure positive dimensions.
   *
   * @param size The original size vector
   * @return The transformed size vector with non-negative X and Z components
   */
  public Vec3i applyToSizeInverted(Vec3i size) {
    var rotated = applyRotationInverted(size);
    return new Vec3i(Math.abs(rotated.getX()), rotated.getY(), Math.abs(rotated.getZ()));
  }

  /**
   * Translates the world origin by the translation offset of this transform.
   *
   * @return The translated world origin as a BlockPos
   */
  public BlockPos getTranslatedOrigin() {
    return new BlockPos(translation);
  }

  /**
   * Checks if this transform includes mirroring or rotation.
   *
   * @return True if this transform includes mirroring or rotation, false otherwise
   */
  public boolean isMirroredOrRotated() {
    return mirror != Mirror.NONE || rotation != Rotation.NONE;
  }

  /**
   * Applies the mirror transformation to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The mirrored Vec3i
   */
  public Vec3i applyMirror(Vec3i vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3i(-vec.getX(), vec.getY(), vec.getZ());
      case FRONT_BACK -> new Vec3i(vec.getZ(), vec.getY(), -vec.getX());
    };
  }

  /**
   * Applies the mirror transformation to a Vec3.
   *
   * @param vec The original Vec3
   * @return The mirrored Vec3
   */
  private Vec3 applyMirror(Vec3 vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3(-vec.x, vec.y, vec.z);
      case FRONT_BACK -> new Vec3(vec.z, vec.y, -vec.x);
    };
  }

  /**
   * Applies the mirror transformation to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The mirrored BlockPos
   */
  public BlockPos applyMirror(BlockPos pos) {
    return switch (mirror) {
      case NONE -> pos;
      case LEFT_RIGHT -> new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
      case FRONT_BACK -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
    };
  }

  /**
   * Applies the mirror transformation to a BlockState.
   *
   * @param blockState The original BlockState
   * @return The mirrored BlockState
   */
  public BlockState applyMirror(BlockState blockState) {
    return blockState.mirror(mirror);
  }

  /**
   * Applies the rotation transformation to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The rotated Vec3i
   */
  public Vec3i applyRotation(Vec3i vec) {
    return rotate(rotation, vec);
  }

  /**
   * Applies the rotation transformation to a Vec3.
   *
   * @param vec The original Vec3
   * @return The rotated Vec3
   */
  public Vec3 applyRotation(Vec3 vec) {
    return rotate(rotation, vec);
  }

  /**
   * Applies the rotation transformation to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The rotated BlockPos
   */
  public BlockPos applyRotation(BlockPos pos) {
    return pos.rotate(rotation);
  }

  /**
   * Applies the rotation transformation to a BlockState.
   *
   * @param blockState The original BlockState
   * @return The rotated BlockState
   */
  public BlockState applyRotation(BlockState blockState) {
    return blockState.rotate(rotation);
  }

  /**
   * Applies the inverted rotation transformation to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The inversely rotated Vec3i
   */
  public Vec3i applyRotationInverted(Vec3i vec) {
    return rotate(invert(rotation), vec);
  }

  /**
   * Applies the inverted rotation transformation to a Vec3.
   *
   * @param vec The original Vec3
   * @return The inversely rotated Vec3
   */
  private Vec3 applyRotationInverted(Vec3 vec) {
    return rotate(invert(rotation), vec);
  }

  /**
   * Applies the inverted rotation transformation to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The inversely rotated BlockPos
   */
  public BlockPos applyRotationInverted(BlockPos pos) {
    return pos.rotate(invert(rotation));
  }

  /**
   * Applies the inverted rotation transformation to a BlockState.
   *
   * @param blockState The original BlockState
   * @return The inversely rotated BlockState
   */
  public BlockState applyRotationInverted(BlockState blockState) {
    return blockState.rotate(invert(rotation));
  }

  /**
   * Applies the translation transformation to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The translated Vec3i
   */
  public Vec3i applyTranslation(Vec3i vec) {
    return vec.offset(translation);
  }

  /**
   * Applies the translation transformation to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The translated BlockPos
   */
  public BlockPos applyTranslation(BlockPos pos) {
    return pos.offset(translation);
  }

  /**
   * Applies the inverted translation transformation to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The inversely translated Vec3i
   */
  public Vec3i applyTranslationInverted(Vec3i vec) {
    return vec.subtract(translation);
  }

  /**
   * Applies the inverted translation transformation to a Vec3.
   *
   * @param vec The original Vec3
   * @return The inversely translated Vec3
   */
  private Vec3 applyTranslationInverted(Vec3 vec) {
    return vec.subtract(translation.getX(), translation.getY(), translation.getZ());
  }

  /**
   * Applies the inverted translation transformation to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The inversely translated BlockPos
   */
  public BlockPos applyTranslationInverted(BlockPos pos) {
    return pos.subtract(translation);
  }

  /**
   * Applies all transformations (mirror, rotate, translate) to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The transformed Vec3i
   */
  public Vec3i apply(Vec3i vec) {
    vec = applyMirror(vec);
    vec = applyRotation(vec);
    vec = applyTranslation(vec);
    return vec;
  }

  /**
   * Applies all transformations (mirror, rotate, translate) to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The transformed BlockPos
   */
  public BlockPos apply(BlockPos pos) {
    pos = applyMirror(pos);
    pos = applyRotation(pos);
    pos = applyTranslation(pos);
    return pos;
  }

  /**
   * Applies mirror and rotation transformations to a BlockState.
   *
   * <p>Note: Translation does not affect BlockState.
   *
   * @param blockState The original BlockState
   * @return The transformed BlockState
   */
  public BlockState apply(BlockState blockState) {
    return blockState.mirror(mirror).rotate(rotation);
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a Vec3i.
   *
   * @param vec The original Vec3i
   * @return The inversely transformed Vec3i
   */
  public Vec3i applyInverted(Vec3i vec) {
    vec = applyTranslationInverted(vec);
    vec = applyRotationInverted(vec);
    vec = applyMirror(vec);
    return vec;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a Vec3.
   *
   * @param vec The original Vec3
   * @return The inversely transformed Vec3
   */
  public Vec3 applyInverted(Vec3 vec) {
    vec = applyTranslationInverted(vec);
    vec = applyRotationInverted(vec);
    vec = applyMirror(vec);
    return vec;
  }

  /**
   * Applies the inverted transformations (translate, rotate, mirror) to a BlockPos.
   *
   * @param pos The original BlockPos
   * @return The inversely transformed BlockPos
   */
  public BlockPos applyInverted(BlockPos pos) {
    pos = applyTranslationInverted(pos);
    pos = applyRotationInverted(pos);
    pos = applyMirror(pos);
    return pos;
  }

  /**
   * Applies the inverted transformations (mirror, rotate) to a BlockState.
   *
   * <p>Note: Translation does not affect BlockState.
   *
   * @param blockState The original BlockState
   * @return The inversely transformed BlockState
   */
  public BlockState applyInverted(BlockState blockState) {
    return blockState.rotate(invert(rotation)).mirror(mirror);
  }

  /**
   * Rotates a Vec3i by the specified rotation.
   *
   * @param rotation The rotation to apply
   * @param vec The original Vec3i
   * @return The rotated Vec3i
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
   * Rotates a Vec3 by the specified rotation.
   *
   * @param rotation The rotation to apply
   * @param vec The original Vec3
   * @return The rotated Vec3
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
   * @param rotation The rotation to invert
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
