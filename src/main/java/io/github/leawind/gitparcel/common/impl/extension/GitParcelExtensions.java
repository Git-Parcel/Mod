package io.github.leawind.gitparcel.common.impl.extension;

import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Discovers and commits Git Parcel extensions. */
public final class GitParcelExtensions {
  private static final Logger LOGGER = LoggerFactory.getLogger(GitParcelExtensions.class);
  private GitParcelExtensions() {}

  public static void discoverAndFreeze() {
    var registry = ParcelContentTypeRegistry.get();
    var extensions = new ArrayList<GitParcelExtension>();
    ServiceLoader<GitParcelExtension> loader =
        ServiceLoader.load(GitParcelExtension.class, GitParcelExtension.class.getClassLoader());

    for (var provider : loader.stream().toList()) {
      try {
        extensions.add(provider.get());
      } catch (ServiceConfigurationError | RuntimeException e) {
        LOGGER.error("Failed to instantiate Git Parcel extension {}", provider.type().getName(), e);
      }
    }

    extensions.sort(
        Comparator.comparing(GitParcelExtension::id)
            .thenComparing(extension -> extension.getClass().getName()));

    Set<String> loadedIds = new HashSet<>();
    for (var extension : extensions) {
      if (!loadedIds.add(extension.id())) {
        LOGGER.error(
            "Disabling duplicate Git Parcel extension {} ({})",
            extension.id(),
            extension.getClass().getName());
        continue;
      }

      var registrar = new ParcelExtensionRegistrarImpl();
      try {
        extension.register(registrar);
        registrar.commit(registry);
        LOGGER.debug(
            "Loaded Git Parcel extension {} ({})",
            extension.id(),
            extension.getClass().getName());
      } catch (RuntimeException e) {
        LOGGER.error(
            "Disabling Git Parcel extension {} ({})",
            extension.id(),
            extension.getClass().getName(),
            e);
      }
    }

    ParcelRecordProcessorRegistry.get().freeze();
    ParcelAttachmentTypeRegistry.get().freeze();
    registry.freeze();
  }
}
