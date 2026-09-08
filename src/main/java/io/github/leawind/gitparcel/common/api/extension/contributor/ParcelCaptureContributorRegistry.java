package io.github.leawind.gitparcel.common.api.extension.contributor;

import java.util.List;

/** Registry of parcel capture contributors, frozen after extension discovery. */
public interface ParcelCaptureContributorRegistry {
  static ParcelCaptureContributorRegistry get() {
    return io.github.leawind.gitparcel.common.impl.extension.contributor.ParcelCaptureContributorRegistryImpl.INSTANCE;
  }

  void register(ParcelCaptureContributor contributor);

  List<ParcelCaptureContributor> contributors();

  void freeze();

  boolean isFrozen();
}
