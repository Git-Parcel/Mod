package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.git.GitOperationSnapshot;
import java.util.List;
import java.util.Optional;

/** Replaces the client's permission-filtered view of recent asynchronous Git operations. */
public record UpdateGitOperationsMessage(
    List<GitOperationSnapshot> operations, Optional<String> error) implements ServerMessage {
  public static final Codec<UpdateGitOperationsMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      GitOperationSnapshot.CODEC
                          .listOf()
                          .fieldOf("operations")
                          .forGetter(UpdateGitOperationsMessage::operations),
                      Codec.STRING
                          .optionalFieldOf("error")
                          .forGetter(UpdateGitOperationsMessage::error))
                  .apply(instance, UpdateGitOperationsMessage::new));

  public UpdateGitOperationsMessage {
    operations = List.copyOf(operations);
    error = error == null ? Optional.empty() : error;
  }
}
