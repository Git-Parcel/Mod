package io.github.leawind.gitparcel.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TransformUtils {
  /**
   * Inverts the specified rotation.
   *
   * @param rotation The rotation
   * @return The inverted rotation
   */
  public static Rotation invert(Rotation rotation) {
    return switch (rotation) {
      case NONE, CLOCKWISE_180 -> rotation;
      case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
      case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
    };
  }

  /**
   * Rotates a {@link Vec3i} by the specified rotation.
   *
   * @param rotation The rotation
   * @param vec The vector
   * @return The rotated vector
   */
  public static Vec3i rotate(Rotation rotation, Vec3i vec) {
    return switch (rotation) {
      case NONE -> vec;
      case CLOCKWISE_90 -> new Vec3i(-vec.getZ(), vec.getY(), vec.getX());
      case CLOCKWISE_180 -> new Vec3i(-vec.getX(), vec.getY(), -vec.getZ());
      case COUNTERCLOCKWISE_90 -> new Vec3i(vec.getZ(), vec.getY(), -vec.getX());
    };
  }

  public static Vec3i rotateInverted(Rotation rotation, Vec3i vec) {
    return TransformUtils.rotate(TransformUtils.invert(rotation), vec);
  }

  /**
   * Rotates a {@link Vec3} by the specified rotation.
   *
   * @param rotation The rotation
   * @param vec The vector
   * @return The rotated vector
   */
  public static Vec3 rotate(Rotation rotation, Vec3 vec) {
    return switch (rotation) {
      case NONE -> vec;
      case CLOCKWISE_90 -> new Vec3(-vec.z, vec.y, vec.x);
      case CLOCKWISE_180 -> new Vec3(-vec.x, vec.y, -vec.z);
      case COUNTERCLOCKWISE_90 -> new Vec3(vec.z, vec.y, -vec.x);
    };
  }

  public static Vec3 rotateInverted(Rotation rotation, Vec3 vec) {
    return TransformUtils.rotate(TransformUtils.invert(rotation), vec);
  }

  public static BlockPos rotateInverted(Rotation rotation, BlockPos pos) {
    return pos.rotate(TransformUtils.invert(rotation));
  }

  public static void rotateY(Rotation rotation, Matrix4f matrix) {
    switch (rotation) {
      case CLOCKWISE_90 -> matrix.rotateY((float) Math.toRadians(-90));
      case CLOCKWISE_180 -> matrix.rotateY((float) Math.toRadians(180));
      case COUNTERCLOCKWISE_90 -> matrix.rotateY((float) Math.toRadians(90));
    }
  }

  public static Vec3 mirror(Mirror mirror, Vec3 vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3(vec.x, vec.y, -vec.z);
      case FRONT_BACK -> new Vec3(-vec.x, vec.y, vec.z);
    };
  }

  public static Vec3i mirror(Mirror mirror, Vec3i vec) {
    return switch (mirror) {
      case NONE -> vec;
      case LEFT_RIGHT -> new Vec3i(vec.getX(), vec.getY(), -vec.getZ());
      case FRONT_BACK -> new Vec3i(-vec.getX(), vec.getY(), vec.getZ());
    };
  }

  public static BlockPos mirror(Mirror mirror, BlockPos pos) {
    return switch (mirror) {
      case NONE -> pos;
      case FRONT_BACK -> new BlockPos(-pos.getX(), pos.getY(), pos.getZ());
      case LEFT_RIGHT -> new BlockPos(pos.getX(), pos.getY(), -pos.getZ());
    };
  }

  public static void mirror(Mirror mirror, Matrix4f matrix) {
    switch (mirror) {
      case LEFT_RIGHT -> matrix.scale(1, 1, -1);
      case FRONT_BACK -> matrix.scale(-1, 1, 1);
    }
  }

  public static void translate(Vec3i translation, Matrix4f matrix) {
    matrix.translate(translation.getX(), translation.getY(), translation.getZ());
  }

  public static Vec3i translate(Vec3i translation, Vec3i vec) {
    return vec.offset(translation);
  }

  public static BlockPos translate(Vec3i translation, BlockPos pos) {
    return pos.offset(translation);
  }

  public static Vec3 translateInverted(Vec3i translation, Vec3 vec) {
    return vec.subtract(translation.getX(), translation.getY(), translation.getZ());
  }

  public static Vec3i translateInverted(Vec3i translation, Vec3i vec) {
    return vec.subtract(translation);
  }

  public static BlockPos translateInverted(Vec3i translation, BlockPos pos) {
    return pos.subtract(translation);
  }
}
