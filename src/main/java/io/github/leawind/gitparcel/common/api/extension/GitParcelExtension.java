package io.github.leawind.gitparcel.common.api.extension;

/**
 * Entry point for Git Parcel extensions discovered through {@link java.util.ServiceLoader}.
 *
 * <p>This API is available for source-level experimentation while Git Parcel is in development. It
 * does not currently carry a binary compatibility guarantee.
 */
public interface GitParcelExtension {
  /** A stable, namespaced identifier such as {@code examplemod:integration}. */
  String id();

  /** Contributes formats and data processors to an isolated registration transaction. */
  void register(ParcelExtensionRegistrar registrar);
}
