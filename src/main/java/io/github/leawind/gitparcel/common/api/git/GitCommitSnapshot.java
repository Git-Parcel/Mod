package io.github.leawind.gitparcel.common.api.git;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Client-safe summary of one Git commit. */
public record GitCommitSnapshot(
    String revision, String committedAt, String author, String message) {
  public static final Codec<GitCommitSnapshot> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING.fieldOf("revision").forGetter(GitCommitSnapshot::revision),
                      Codec.STRING
                          .fieldOf("committed_at")
                          .forGetter(GitCommitSnapshot::committedAt),
                      Codec.STRING.fieldOf("author").forGetter(GitCommitSnapshot::author),
                      Codec.STRING.fieldOf("message").forGetter(GitCommitSnapshot::message))
                  .apply(instance, GitCommitSnapshot::new));
}
