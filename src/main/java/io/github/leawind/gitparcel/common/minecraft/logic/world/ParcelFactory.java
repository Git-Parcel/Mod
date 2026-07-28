package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.permission.PermissionConfig;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftVersion;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.stream.Collectors;

/** Creates parcel models using the active Minecraft runtime and content registry. */
public final class ParcelFactory {
  private ParcelFactory() {}

  /** Creates metadata stamped with the active Minecraft data version. */
  public static ParcelMeta createMetadata(Vec3i parcelSize, Vec3i anchor) {
    var contents =
        ParcelContentTypeRegistry.get().latestTypes().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    type -> type.spec().id(),
                    type -> defaultManifest(type)));
    return new ParcelMeta(contents, MinecraftVersion.currentDataVersion(), parcelSize, anchor);
  }

  /** Creates metadata from a world-space box and converts its size to parcel-local space. */
  public static ParcelMeta createMetadata(BoundingBox boundingBox, Rotation rotation) {
    var sizeWorldSpace =
        new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
    var sizeParcelSpace = ParcelTransform.rotateSize(rotation, sizeWorldSpace);
    return createMetadata(sizeParcelSpace, Vec3i.ZERO);
  }

  private static ParcelContentManifest defaultManifest(ParcelContentType<?> type) {
    var config = type.defaultConfig();
    return new ParcelContentManifest(
        type.spec().version(), config == null ? null : config.toJson());
  }

  /** Creates a parcel using the active content types. */
  public static Parcel create(BoundingBox boundingBox, Mirror mirror, Rotation rotation) {
    return create(
        boundingBox,
        mirror,
        rotation,
        new PermissionConfig<>(ParcelPermissions.REGISTRY));
  }

  /** Creates a parcel using the active content types and supplied permissions. */
  public static Parcel create(
      BoundingBox boundingBox,
      Mirror mirror,
      Rotation rotation,
      PermissionConfig<ParcelPermissions> permissions) {
    var pivot = Parcel.getPivot(mirror, rotation, boundingBox);
    var transform =
        new ParcelTransform(
            mirror, rotation, new Vec3i((int) pivot.x, (int) pivot.y, (int) pivot.z));

    var meta = createMetadata(boundingBox, rotation);

    return Parcel.create(meta, transform, permissions);
  }
}
