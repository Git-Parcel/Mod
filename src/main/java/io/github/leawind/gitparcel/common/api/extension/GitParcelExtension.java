package io.github.leawind.gitparcel.common.api.extension;

/**
 * Entry point for Git Parcel extensions discovered through {@link java.util.ServiceLoader}.
 *
 * <p>Extensions contribute content types, record processors, attachment types, declared NBT
 * fields (world coordinates and entity references), and capture contributors. See the extension
 * guide in {@code docs/DESIGN.md}.
 *
 * <p>This API is available for source-level experimentation while Git Parcel is in development. It
 * does not currently carry a binary compatibility guarantee.
 */
public interface GitParcelExtension {
  /** A stable, namespaced identifier such as {@code examplemod:integration}. */
  String id();

  /**
   * The namespaces this extension owns for rule 7.4 adjudication: entries registered under an
   * owned namespace take precedence over guest registrations regardless of load order. Defaults
   * to the namespace of {@link #id()}; the built-in extension additionally owns {@code minecraft}
   * because it ships the vanilla-field processors.
   */
  default java.util.Collection<String> ownedNamespaces() {
    String id = id();
    int separator = id.indexOf(':');
    return java.util.Set.of(separator < 0 ? id : id.substring(0, separator));
  }

  /** Contributes content types, record processors, and attachment types in one transaction. */
  void register(ParcelExtensionRegistrar registrar);
}
