package io.github.leawind.gitparcel.common.impl.extension.contributor;

import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributor;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributorRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class ParcelCaptureContributorRegistryImpl
    implements ParcelCaptureContributorRegistry {
  public static final ParcelCaptureContributorRegistryImpl INSTANCE =
      new ParcelCaptureContributorRegistryImpl();

  private final Map<Identifier, ParcelCaptureContributor> contributors = new LinkedHashMap<>();
  private boolean frozen;

  private ParcelCaptureContributorRegistryImpl() {}

  @Override
  public void register(ParcelCaptureContributor contributor) {
    if (frozen) {
      throw new IllegalStateException("Parcel capture contributor registry is frozen");
    }
    if (contributors.putIfAbsent(contributor.id(), contributor) != null) {
      throw new IllegalArgumentException(
          "duplicate parcel capture contributor: " + contributor.id());
    }
  }

  @Override
  public List<ParcelCaptureContributor> contributors() {
    return List.copyOf(contributors.values());
  }

  @Override
  public void freeze() {
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }
}
