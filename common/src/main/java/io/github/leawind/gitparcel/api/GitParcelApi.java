package io.github.leawind.gitparcel.api;

import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;

public class GitParcelApi {
  /** A global registry for parcel formats. */
  public static final ParcelFormatRegistry FORMAT_REGISTRY = new ParcelFormatRegistry();
}
