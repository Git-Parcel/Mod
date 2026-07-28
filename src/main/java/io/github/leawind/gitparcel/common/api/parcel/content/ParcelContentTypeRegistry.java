package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.Factory;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Registry of versioned parcel content implementations. */
public interface ParcelContentTypeRegistry {
  static ParcelContentTypeRegistry get() {
    return Factory.getParcelContentTypeRegistry();
  }

  void register(ParcelContentType<?> type);

  @Nullable ParcelContentType<?> get(ParcelContentType.Spec spec);

  /** Returns the highest registered version for an id. */
  @Nullable ParcelContentType<?> latest(String id);

  /** Returns one highest-version implementation per id, sorted by id. */
  List<ParcelContentType<?>> latestTypes();

  List<ParcelContentType<?>> registeredTypes();

  void clear();

  void freeze();

  boolean isFrozen();
}
