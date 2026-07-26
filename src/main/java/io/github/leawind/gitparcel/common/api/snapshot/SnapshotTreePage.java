package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Stable cursor page of a parcel's complete logical snapshot tree. */
public record SnapshotTreePage(
    UUID parcelUuid,
    Optional<SnapshotId> cursor,
    List<SnapshotNode> nodes,
    Optional<SnapshotId> current,
    Optional<SnapshotId> nextCursor,
    Optional<String> error) {
  public static final Codec<SnapshotTreePage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("parcel_uuid").forGetter(SnapshotTreePage::parcelUuid),
                      SnapshotId.CODEC.optionalFieldOf("cursor").forGetter(SnapshotTreePage::cursor),
                      SnapshotNode.CODEC.listOf().fieldOf("nodes").forGetter(SnapshotTreePage::nodes),
                      SnapshotId.CODEC.optionalFieldOf("current").forGetter(SnapshotTreePage::current),
                      SnapshotId.CODEC
                          .optionalFieldOf("next_cursor")
                          .forGetter(SnapshotTreePage::nextCursor),
                      Codec.STRING.optionalFieldOf("error").forGetter(SnapshotTreePage::error))
                  .apply(instance, SnapshotTreePage::new));

  public SnapshotTreePage {
    if (parcelUuid == null || nodes == null) {
      throw new IllegalArgumentException("Snapshot tree page fields must not be null");
    }
    cursor = cursor == null ? Optional.empty() : cursor;
    nodes = List.copyOf(nodes);
    current = current == null ? Optional.empty() : current;
    nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    error = error == null ? Optional.empty() : error;
  }

  public static SnapshotTreePage failure(UUID parcelUuid, Optional<SnapshotId> cursor, String error) {
    return new SnapshotTreePage(
        parcelUuid, cursor, List.of(), Optional.empty(), Optional.empty(), Optional.of(error));
  }
}
