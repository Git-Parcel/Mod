package io.github.leawind.gitparcel.common.api.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Client-safe projection of any potentially long-running server operation. */
public record OperationSnapshot(
    UUID operationId,
    String kind,
    String owner,
    String target,
    State state,
    String phase,
    long completed,
    Optional<Long> total,
    Optional<String> unit,
    String submittedAt,
    Optional<String> startedAt,
    String updatedAt,
    Optional<String> completedAt,
    Optional<String> result,
    Optional<String> error) {
  public static final Codec<OperationSnapshot> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("operation_id").forGetter(OperationSnapshot::operationId),
                      Codec.STRING.fieldOf("kind").forGetter(OperationSnapshot::kind),
                      Codec.STRING.fieldOf("owner").forGetter(OperationSnapshot::owner),
                      Codec.STRING.fieldOf("target").forGetter(OperationSnapshot::target),
                      State.CODEC.fieldOf("state").forGetter(OperationSnapshot::state),
                      Codec.STRING.fieldOf("phase").forGetter(OperationSnapshot::phase),
                      Codec.LONG.fieldOf("completed").forGetter(OperationSnapshot::completed),
                      Codec.LONG.optionalFieldOf("total").forGetter(OperationSnapshot::total),
                      Codec.STRING.optionalFieldOf("unit").forGetter(OperationSnapshot::unit),
                      Codec.STRING.fieldOf("submitted_at").forGetter(OperationSnapshot::submittedAt),
                      Codec.STRING.optionalFieldOf("started_at").forGetter(OperationSnapshot::startedAt),
                      Codec.STRING.fieldOf("updated_at").forGetter(OperationSnapshot::updatedAt),
                      Codec.STRING.optionalFieldOf("completed_at").forGetter(OperationSnapshot::completedAt),
                      Codec.STRING.optionalFieldOf("result").forGetter(OperationSnapshot::result),
                      Codec.STRING.optionalFieldOf("error").forGetter(OperationSnapshot::error))
                  .apply(instance, OperationSnapshot::new));

  public OperationSnapshot {
    if (operationId == null || state == null) {
      throw new IllegalArgumentException("Operation identity and state must not be null");
    }
    kind = requireText(kind, "Operation kind");
    owner = requireText(owner, "Operation owner");
    target = requireText(target, "Operation target");
    phase = requireText(phase, "Operation phase");
    submittedAt = requireText(submittedAt, "Operation submission time");
    updatedAt = requireText(updatedAt, "Operation update time");
    if (completed < 0) {
      throw new IllegalArgumentException("Operation progress must not be negative");
    }
    total = total == null ? Optional.empty() : total;
    unit = unit == null ? Optional.empty() : unit;
    startedAt = startedAt == null ? Optional.empty() : startedAt;
    completedAt = completedAt == null ? Optional.empty() : completedAt;
    result = result == null ? Optional.empty() : result;
    error = error == null ? Optional.empty() : error;
    if (total.isPresent() && (total.orElseThrow() < 0 || total.orElseThrow() < completed)) {
      throw new IllegalArgumentException("Operation total must be at least completed");
    }
  }

  private static String requireText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return value;
  }

  public enum State {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public static final Codec<State> CODEC =
        Codec.STRING.xmap(
            value -> State.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));

    public boolean isTerminal() {
      return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
  }
}
