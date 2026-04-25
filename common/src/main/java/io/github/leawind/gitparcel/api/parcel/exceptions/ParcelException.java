package io.github.leawind.gitparcel.api.parcel.exceptions;

import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.Arrays;

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
    public final ParcelFormat.Info formatInfo;

    public UnsupportedFormat(ParcelFormat.Info formatInfo) {
      super(String.format("Unsupported format: %s", formatInfo));
      this.formatInfo = formatInfo;
    }
  }

  public static class UnsupportedFeature extends ParcelException {
    public final ParcelFormat.Info formatInfo;
    public final ParcelFormat.Feature[] features;

    public UnsupportedFeature(ParcelFormat.Info formatInfo, ParcelFormat.Feature... features) {
      super(
          String.format(
              "Unsupported features for format %s: %s", formatInfo, Arrays.toString(features)));
      this.formatInfo = formatInfo;
      this.features = features;
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
