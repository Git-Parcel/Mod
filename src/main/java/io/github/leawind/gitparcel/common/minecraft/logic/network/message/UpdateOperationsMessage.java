package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.operation.OperationSnapshot;
import java.util.List;
import java.util.Optional;

/** Replaces the client's permission-filtered view of recent asynchronous operations. */
public record UpdateOperationsMessage(
    List<OperationSnapshot> operations, Optional<String> error) implements ServerMessage {
  public static final Codec<UpdateOperationsMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      OperationSnapshot.CODEC
                          .listOf()
                          .fieldOf("operations")
                          .forGetter(UpdateOperationsMessage::operations),
                      Codec.STRING
                          .optionalFieldOf("error")
                          .forGetter(UpdateOperationsMessage::error))
                  .apply(instance, UpdateOperationsMessage::new));

  public UpdateOperationsMessage {
    operations = List.copyOf(operations);
    error = error == null ? Optional.empty() : error;
  }
}
