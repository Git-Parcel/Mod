package io.github.leawind.gitparcel.common.api.git;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

/** Client-safe summary of one server-managed shared repository. */
public record SharedRepositorySnapshot(
    String name,
    String type,
    Optional<String> remoteUrl,
    Optional<String> lastSync,
    List<String> parcelPaths) {
  public static final Codec<SharedRepositorySnapshot> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING.fieldOf("name").forGetter(SharedRepositorySnapshot::name),
                      Codec.STRING.fieldOf("type").forGetter(SharedRepositorySnapshot::type),
                      Codec.STRING
                          .optionalFieldOf("remote_url")
                          .forGetter(SharedRepositorySnapshot::remoteUrl),
                      Codec.STRING
                          .optionalFieldOf("last_sync")
                          .forGetter(SharedRepositorySnapshot::lastSync),
                      Codec.STRING
                          .listOf()
                          .fieldOf("parcel_paths")
                          .forGetter(SharedRepositorySnapshot::parcelPaths))
                  .apply(instance, SharedRepositorySnapshot::new));

  public SharedRepositorySnapshot {
    remoteUrl = remoteUrl == null ? Optional.empty() : remoteUrl;
    lastSync = lastSync == null ? Optional.empty() : lastSync;
    parcelPaths = List.copyOf(parcelPaths);
  }
}
