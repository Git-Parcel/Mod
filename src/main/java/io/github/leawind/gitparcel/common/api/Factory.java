package io.github.leawind.gitparcel.common.api;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import io.github.leawind.gitparcel.common.impl.parcel.ParcelContentTypeRegistryImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class Factory {
  private Factory() {}

  public static ParcelContentTypeRegistry getParcelContentTypeRegistry() {
    return ParcelContentTypeRegistryImpl.INSTANCE;
  }
}
