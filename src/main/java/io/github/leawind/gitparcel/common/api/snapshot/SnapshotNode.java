package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;

/** Read-only projection of one immutable snapshot commit. */
public record SnapshotNode(
    SnapshotId id,
    Optional<SnapshotId> parentId,
    String name,
    String description,
    String author,
    String createdAt,
    Source source,
    ContentSummary content) {
  public static final Codec<SnapshotNode> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      SnapshotId.CODEC.fieldOf("id").forGetter(SnapshotNode::id),
                      SnapshotId.CODEC
                          .optionalFieldOf("parent_id")
                          .forGetter(SnapshotNode::parentId),
                      Codec.STRING.fieldOf("name").forGetter(SnapshotNode::name),
                      Codec.STRING.fieldOf("description").forGetter(SnapshotNode::description),
                      Codec.STRING.fieldOf("author").forGetter(SnapshotNode::author),
                      Codec.STRING.fieldOf("created_at").forGetter(SnapshotNode::createdAt),
                      Source.CODEC.fieldOf("source").forGetter(SnapshotNode::source),
                      ContentSummary.CODEC.fieldOf("content").forGetter(SnapshotNode::content))
                  .apply(instance, SnapshotNode::new));

  public SnapshotNode {
    if (id == null || source == null || content == null) {
      throw new IllegalArgumentException("Snapshot node fields must not be null");
    }
    parentId = parentId == null ? Optional.empty() : parentId;
    name = requireText(name, "Snapshot name");
    description = description == null ? "" : description;
    author = requireText(author, "Snapshot author");
    createdAt = requireText(createdAt, "Snapshot time");
  }

  private static String requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }

  public enum Source {
    SAVED,
    IMPORTED;

    public static final Codec<Source> CODEC =
        Codec.STRING.xmap(
            value -> Source.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
  }

  public record ContentSummary(long files, long bytes) {
    public static final Codec<ContentSummary> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.LONG.fieldOf("files").forGetter(ContentSummary::files),
                        Codec.LONG.fieldOf("bytes").forGetter(ContentSummary::bytes))
                    .apply(instance, ContentSummary::new));

    public ContentSummary {
      if (files < 0 || bytes < 0) {
        throw new IllegalArgumentException("Snapshot content counts must be non-negative");
      }
    }
  }
}
