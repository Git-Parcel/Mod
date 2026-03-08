package io.github.leawind.gitparcel.api.parcel.exceptions;

/** Custom exception for parcel-related errors. */
public class ParcelException extends Exception {
  public ParcelException(String message) {
    super(message);
  }

  public ParcelException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Exception thrown when a parcel format is not supported */
  public static class UnsupportedFormat extends ParcelException {
    public final String formatId;
    public final int formatVersion;

    public UnsupportedFormat(String formatId, int formatVersion) {
      super(String.format("Unsupported format: %s:%d", formatId, formatVersion));
      this.formatId = formatId;
      this.formatVersion = formatVersion;
    }
  }

  /** Exception thrown when a parcel format has fatal errors that cannot be recovered from */
  public static class CorruptedParcelException extends ParcelException {
    public CorruptedParcelException(String message) {
      super(message);
    }

    public CorruptedParcelException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
