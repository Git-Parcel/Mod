package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;

@FunctionalInterface
public interface ParcelContentConsumer<T> {
  void accept(T value) throws IOException, ParcelException;
}
