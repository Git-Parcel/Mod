package io.github.leawind.gitparcel.common.api.snapshot;

import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.regex.Pattern;

/** Opaque, client-safe identity of a snapshot commit. */
public record SnapshotId(String value) implements Comparable<SnapshotId> {
  private static final Pattern OBJECT_ID = Pattern.compile("^[0-9a-f]{40,128}$");
  public static final Codec<SnapshotId> CODEC = Codec.STRING.xmap(SnapshotId::new, SnapshotId::value);

  public SnapshotId {
    if (value == null) {
      throw new IllegalArgumentException("Snapshot ID must not be null");
    }
    value = value.toLowerCase(Locale.ROOT);
    if ((value.length() & 1) != 0 || !OBJECT_ID.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid snapshot object ID");
    }
  }

  public String abbreviate() {
    return value.substring(0, Math.min(8, value.length()));
  }

  @Override
  public int compareTo(SnapshotId other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value;
  }
}
