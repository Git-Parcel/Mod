package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftVersion;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Creates parcel models using the active Minecraft runtime and format registry. */
public final class ParcelFactory {
  private ParcelFactory() {}

  /** Creates metadata stamped with the active Minecraft data version. */
  public static ParcelMeta createMetadata(
      ParcelFormat.Spec format, Vec3i parcelSize, Vec3i anchor) {
    return new ParcelMeta(format, MinecraftVersion.currentDataVersion(), parcelSize, anchor);
  }

  /** Creates metadata from a world-space box and converts its size to parcel-local space. */
  public static ParcelMeta createMetadata(
      ParcelFormat.Spec format, BoundingBox boundingBox, Rotation rotation) {
    var sizeWorldSpace =
        new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
    var sizeParcelSpace = ParcelTransform.rotateSize(rotation, sizeWorldSpace);
    return createMetadata(format, sizeParcelSpace, Vec3i.ZERO);
  }

  /** Creates a parcel using the active default format writer. */
  public static Parcel create(BoundingBox boundingBox, Mirror mirror, Rotation rotation) {
    var pivot = Parcel.getPivot(mirror, rotation, boundingBox);
    var transform =
        new ParcelTransform(
            mirror, rotation, new Vec3i((int) pivot.x, (int) pivot.y, (int) pivot.z));

    var format = ParcelFormatRegistry.get().defaultWriter().spec();
    var meta = createMetadata(format, boundingBox, rotation);

    return Parcel.create(meta, transform);
  }
}
