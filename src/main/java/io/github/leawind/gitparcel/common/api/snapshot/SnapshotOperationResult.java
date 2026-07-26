package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Structured result shared by commands and future player UI. */
public record SnapshotOperationResult(
    UUID operationId,
    Status status,
    Optional<SnapshotId> snapshotId,
    ErrorCategory errorCategory,
    String message) {
  public static final Codec<SnapshotOperationResult> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("operation_id").forGetter(SnapshotOperationResult::operationId),
                      Status.CODEC.fieldOf("status").forGetter(SnapshotOperationResult::status),
                      SnapshotId.CODEC.optionalFieldOf("snapshot_id").forGetter(SnapshotOperationResult::snapshotId),
                      ErrorCategory.CODEC.fieldOf("error_category").forGetter(SnapshotOperationResult::errorCategory),
                      Codec.STRING.fieldOf("message").forGetter(SnapshotOperationResult::message))
                  .apply(instance, SnapshotOperationResult::new));

  public SnapshotOperationResult {
    if (operationId == null || status == null || errorCategory == null) {
      throw new IllegalArgumentException("Operation result fields must not be null");
    }
    snapshotId = snapshotId == null ? Optional.empty() : snapshotId;
    message = message == null ? "" : message;
    if (status == Status.SUCCEEDED && errorCategory != ErrorCategory.NONE) {
      throw new IllegalArgumentException("Successful operation cannot have an error category");
    }
  }

  public enum Status {
    SUCCEEDED,
    FAILED,
    NEEDS_RECOVERY;

    public static final Codec<Status> CODEC = enumCodec(Status::valueOf);
  }

  public enum ErrorCategory {
    NONE,
    NOT_FOUND,
    PERMISSION_DENIED,
    BUSY,
    INVALID_REQUEST,
    UNSUPPORTED_FORMAT,
    INVALID_SNAPSHOT,
    REPOSITORY_CORRUPT,
    CONCURRENT_UPDATE,
    WORLD_WRITE_FAILED,
    IO_FAILURE,
    INTERNAL_FAILURE;

    public static final Codec<ErrorCategory> CODEC = enumCodec(ErrorCategory::valueOf);
  }

  private static <T extends Enum<T>> Codec<T> enumCodec(java.util.function.Function<String, T> parser) {
    return Codec.STRING.xmap(
        value -> parser.apply(value.toUpperCase(Locale.ROOT)),
        value -> value.name().toLowerCase(Locale.ROOT));
  }
}
