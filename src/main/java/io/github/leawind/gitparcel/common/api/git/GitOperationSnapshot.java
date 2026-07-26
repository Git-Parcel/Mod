package io.github.leawind.gitparcel.common.api.git;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/** Client-safe snapshot of one asynchronous server Git operation. */
public record GitOperationSnapshot(
    long id,
    String type,
    String repository,
    String requestedBy,
    String status,
    String submittedAt,
    Optional<String> startedAt,
    Optional<String> completedAt,
    String detail) {
  public static final Codec<GitOperationSnapshot> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.LONG.fieldOf("id").forGetter(GitOperationSnapshot::id),
                      Codec.STRING.fieldOf("type").forGetter(GitOperationSnapshot::type),
                      Codec.STRING
                          .fieldOf("repository")
                          .forGetter(GitOperationSnapshot::repository),
                      Codec.STRING
                          .fieldOf("requested_by")
                          .forGetter(GitOperationSnapshot::requestedBy),
                      Codec.STRING.fieldOf("status").forGetter(GitOperationSnapshot::status),
                      Codec.STRING
                          .fieldOf("submitted_at")
                          .forGetter(GitOperationSnapshot::submittedAt),
                      Codec.STRING
                          .optionalFieldOf("started_at")
                          .forGetter(GitOperationSnapshot::startedAt),
                      Codec.STRING
                          .optionalFieldOf("completed_at")
                          .forGetter(GitOperationSnapshot::completedAt),
                      Codec.STRING.fieldOf("detail").forGetter(GitOperationSnapshot::detail))
                  .apply(instance, GitOperationSnapshot::new));

  public GitOperationSnapshot {
    startedAt = startedAt == null ? Optional.empty() : startedAt;
    completedAt = completedAt == null ? Optional.empty() : completedAt;
  }
}
