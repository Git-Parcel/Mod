package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Intent to capture the live parcel and create an immutable snapshot. */
public record SaveSnapshotRequest(
    UUID parcelUuid, String name, String description, boolean ignoreEntities) {
  public static final Codec<SaveSnapshotRequest> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("parcel_uuid").forGetter(SaveSnapshotRequest::parcelUuid),
                      Codec.STRING.fieldOf("name").forGetter(SaveSnapshotRequest::name),
                      Codec.STRING.fieldOf("description").forGetter(SaveSnapshotRequest::description),
                      Codec.BOOL.fieldOf("ignore_entities").forGetter(SaveSnapshotRequest::ignoreEntities))
                  .apply(instance, SaveSnapshotRequest::new));

  public SaveSnapshotRequest {
    if (parcelUuid == null) {
      throw new IllegalArgumentException("Parcel UUID must not be null");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Snapshot name must not be blank");
    }
    description = description == null ? "" : description;
  }
}
