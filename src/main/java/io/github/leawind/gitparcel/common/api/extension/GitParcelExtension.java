package io.github.leawind.gitparcel.common.api.extension;

/**
 * Entry point for Git Parcel extensions discovered through {@link java.util.ServiceLoader}.
 *
 * <p>Extensions contribute content types, record processors, attachment types, declared NBT
 * fields (world coordinates and entity references), and capture contributors. See
 * {@code docs/EXTENSIONS.md} for the integrator guide.
 *
 * <p>This API is available for source-level experimentation while Git Parcel is in development. It
 * does not currently carry a binary compatibility guarantee.
 */
public interface GitParcelExtension {
  /** A stable, namespaced identifier such as {@code examplemod:integration}. */
  String id();

  /** Contributes content types, record processors, and attachment types in one transaction. */
  void register(ParcelExtensionRegistrar registrar);
}
