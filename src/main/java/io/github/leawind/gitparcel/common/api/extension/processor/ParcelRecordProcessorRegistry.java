package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.impl.extension.processor.ParcelRecordProcessorRegistryImpl;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface ParcelRecordProcessorRegistry {
  static ParcelRecordProcessorRegistry get() {
    return ParcelRecordProcessorRegistryImpl.INSTANCE;
  }

  void register(ParcelRecordProcessor processor);

  @Nullable ParcelRecordProcessor get(Identifier id);

  List<ParcelRecordProcessor> orderedProcessors();

  void freeze();

  boolean isFrozen();
}
