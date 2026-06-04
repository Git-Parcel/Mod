package io.github.leawind.gitparcel.core.api.parcel;

import io.github.leawind.gitparcel.core.bridge.level.block.Mirror;
import io.github.leawind.gitparcel.core.bridge.level.block.Rotation;
import io.github.leawind.gitparcel.core.bridge.math.MathBridge;
import net.minecraft.core.Vec3i;

public record ParcelTransform(Mirror mirror, Rotation rotation, MathBridge.Vec3i translation) {
  // TODO

  public static Vec3i rotateSize(Rotation rotation, Vec3i size) {
    return switch (rotation) {
      case NONE, CLOCKWISE_180 -> size;
      case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(size.getZ(), size.getY(), size.getX());
    };
  }
}
