package io.github.leawind.gitparcel.common.minecraft.logic.transform;

import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.utils.TransformUtils;
import org.joml.Matrix4f;

/** Projects a parcel transform into the matrix representation used by rendering code. */
public final class ParcelTransformMatrix {
  private ParcelTransformMatrix() {}

  public static Matrix4f create(ParcelTransform transform) {
    var matrix = new Matrix4f();
    apply(transform, matrix);
    return matrix;
  }

  public static void apply(ParcelTransform transform, Matrix4f matrix) {
    TransformUtils.mirror(transform.mirror(), matrix);
    TransformUtils.rotateY(transform.rotation(), matrix);
    TransformUtils.translate(transform.translation(), matrix);
  }
}
