package io.github.leawind.gitparcel.common.api.parcel.content;

import java.util.regex.Pattern;

/** Parcel-local attachment identity. */
public record LocalAttachmentId(String value) implements Comparable<LocalAttachmentId> {
  private static final Pattern PATTERN = Pattern.compile("[a-zA-Z0-9_.-]{1,128}");

  public LocalAttachmentId {
    if (!PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid local attachment id: " + value);
    }
  }

  @Override
  public int compareTo(LocalAttachmentId other) {
    return value.compareTo(other.value);
  }
}
