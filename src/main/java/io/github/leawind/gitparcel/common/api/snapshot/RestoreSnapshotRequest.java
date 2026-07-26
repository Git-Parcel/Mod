package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Intent to restore an authenticated snapshot belonging to one parcel. */
public record RestoreSnapshotRequest(
    UUID parcelUuid, SnapshotId snapshotId, Mode mode, boolean ignoreEntities) {
  public static final Codec<RestoreSnapshotRequest> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("parcel_uuid").forGetter(RestoreSnapshotRequest::parcelUuid),
                      SnapshotId.CODEC.fieldOf("snapshot_id").forGetter(RestoreSnapshotRequest::snapshotId),
                      Mode.CODEC.fieldOf("mode").forGetter(RestoreSnapshotRequest::mode),
                      Codec.BOOL.fieldOf("ignore_entities").forGetter(RestoreSnapshotRequest::ignoreEntities))
                  .apply(instance, RestoreSnapshotRequest::new));

  public RestoreSnapshotRequest {
    if (parcelUuid == null || snapshotId == null || mode == null) {
      throw new IllegalArgumentException("Restore request fields must not be null");
    }
  }

  public enum Mode {
    DIRECT,
    SAVE_THEN_RESTORE;

    public static final Codec<Mode> CODEC =
        Codec.STRING.xmap(
            value -> Mode.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
  }
}
