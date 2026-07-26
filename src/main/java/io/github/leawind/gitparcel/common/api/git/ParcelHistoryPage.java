package io.github.leawind.gitparcel.common.api.git;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** One cursor-based page of a parcel's Git history. */
public record ParcelHistoryPage(
    UUID parcelUuid,
    Optional<String> beforeRevision,
    List<GitCommitSnapshot> commits,
    Optional<String> nextCursor,
    Optional<String> error) {
  public static final Codec<ParcelHistoryPage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("parcel_uuid").forGetter(ParcelHistoryPage::parcelUuid),
                      Codec.STRING
                          .optionalFieldOf("before_revision")
                          .forGetter(ParcelHistoryPage::beforeRevision),
                      GitCommitSnapshot.CODEC
                          .listOf()
                          .fieldOf("commits")
                          .forGetter(ParcelHistoryPage::commits),
                      Codec.STRING
                          .optionalFieldOf("next_cursor")
                          .forGetter(ParcelHistoryPage::nextCursor),
                      Codec.STRING.optionalFieldOf("error").forGetter(ParcelHistoryPage::error))
                  .apply(instance, ParcelHistoryPage::new));

  public ParcelHistoryPage {
    beforeRevision = beforeRevision == null ? Optional.empty() : beforeRevision;
    commits = List.copyOf(commits);
    nextCursor = nextCursor == null ? Optional.empty() : nextCursor;
    error = error == null ? Optional.empty() : error;
  }

  public static ParcelHistoryPage failure(
      UUID parcelUuid, Optional<String> beforeRevision, String error) {
    return new ParcelHistoryPage(
        parcelUuid, beforeRevision, List.of(), Optional.empty(), Optional.of(error));
  }
}
