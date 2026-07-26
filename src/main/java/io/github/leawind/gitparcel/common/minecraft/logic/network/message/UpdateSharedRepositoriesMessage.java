package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.git.SharedRepositorySnapshot;
import java.util.List;
import java.util.Optional;

/** Replaces the client's permission-filtered view of shared repositories. */
public record UpdateSharedRepositoriesMessage(
    List<SharedRepositorySnapshot> repositories, Optional<String> error) implements ServerMessage {
  public static final Codec<UpdateSharedRepositoriesMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      SharedRepositorySnapshot.CODEC
                          .listOf()
                          .fieldOf("repositories")
                          .forGetter(UpdateSharedRepositoriesMessage::repositories),
                      Codec.STRING
                          .optionalFieldOf("error")
                          .forGetter(UpdateSharedRepositoriesMessage::error))
                  .apply(instance, UpdateSharedRepositoriesMessage::new));

  public UpdateSharedRepositoriesMessage {
    repositories = List.copyOf(repositories);
    error = error == null ? Optional.empty() : error;
  }

  public static UpdateSharedRepositoriesMessage failure(String error) {
    return new UpdateSharedRepositoriesMessage(List.of(), Optional.of(error));
  }
}
