package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.impl.extension.processor.ParcelDataProcessorRegistryImpl;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface ParcelDataProcessorRegistry {
  static ParcelDataProcessorRegistry get() {
    return ParcelDataProcessorRegistryImpl.INSTANCE;
  }

  void register(ParcelDataProcessor processor);

  @Nullable ParcelDataProcessor get(Identifier id);

  List<ParcelDataProcessor> orderedProcessors();

  void freeze();

  boolean isFrozen();
}
