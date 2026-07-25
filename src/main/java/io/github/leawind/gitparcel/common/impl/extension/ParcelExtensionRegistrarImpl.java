package io.github.leawind.gitparcel.common.impl.extension;

import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ParcelExtensionRegistrarImpl implements ParcelExtensionRegistrar {
  private final List<ParcelFormat.Impl<?>> formats = new ArrayList<>();
  private final List<ParcelDataProcessor> processors = new ArrayList<>();
  private final List<ParcelAttachmentType> attachmentTypes = new ArrayList<>();

  @Override
  public void registerFormat(ParcelFormat.Impl<?> format) {
    formats.add(format);
  }

  @Override
  public void registerProcessor(ParcelDataProcessor processor) {
    processors.add(processor);
  }

  @Override
  public void registerAttachmentType(ParcelAttachmentType type) {
    attachmentTypes.add(type);
  }

  void commit(ParcelFormatRegistry registry) {
    Set<ParcelFormat.Spec> writerSpecs = new HashSet<>();
    Set<ParcelFormat.Spec> readerSpecs = new HashSet<>();
    Set<net.minecraft.resources.Identifier> processorIds = new HashSet<>();
    Set<net.minecraft.resources.Identifier> attachmentTypeIds = new HashSet<>();

    for (var format : formats) {
      boolean valid = false;
      if (format instanceof ParcelFormat.Writer<?> writer) {
        valid = true;
        if (!writerSpecs.add(writer.spec()) || registry.getWriter(writer.spec()) != null) {
          throw new IllegalArgumentException("duplicate writer: " + writer.spec());
        }
      }
      if (format instanceof ParcelFormat.Reader<?> reader) {
        valid = true;
        if (!readerSpecs.add(reader.spec()) || registry.getReader(reader.spec()) != null) {
          throw new IllegalArgumentException("duplicate reader: " + reader.spec());
        }
      }
      if (!valid) {
        throw new IllegalArgumentException("format must be either writer or reader: " + format);
      }
    }

    for (var processor : processors) {
      if (!processorIds.add(processor.id())
          || ParcelDataProcessorRegistry.get().get(processor.id()) != null) {
        throw new IllegalArgumentException("duplicate processor: " + processor.id());
      }
    }
    for (var type : attachmentTypes) {
      if (!attachmentTypeIds.add(type.id())
          || ParcelAttachmentTypeRegistry.get().get(type.id()) != null) {
        throw new IllegalArgumentException("duplicate attachment type: " + type.id());
      }
    }

    for (var format : formats) {
      registerUnchecked(registry, format);
    }
    processors.forEach(ParcelDataProcessorRegistry.get()::register);
    attachmentTypes.forEach(ParcelAttachmentTypeRegistry.get()::register);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void registerUnchecked(
      ParcelFormatRegistry registry, ParcelFormat.Impl<?> format) {
    registry.register((ParcelFormat.Impl) format);
  }
}
