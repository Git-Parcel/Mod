package io.github.leawind.gitparcel.common.impl.extension;

import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ParcelExtensionRegistrarImpl implements ParcelExtensionRegistrar {
  private final List<ParcelContentType<?>> contentTypes = new ArrayList<>();
  private final List<ParcelRecordProcessor> processors = new ArrayList<>();
  private final List<ParcelAttachmentType> attachmentTypes = new ArrayList<>();

  @Override
  public void registerContentType(ParcelContentType<?> type) {
    contentTypes.add(type);
  }

  @Override
  public void registerProcessor(ParcelRecordProcessor processor) {
    processors.add(processor);
  }

  @Override
  public void registerAttachmentType(ParcelAttachmentType type) {
    attachmentTypes.add(type);
  }

  void commit(ParcelContentTypeRegistry registry) {
    Set<ParcelContentType.Spec> contentSpecs = new HashSet<>();
    Set<net.minecraft.resources.Identifier> processorIds = new HashSet<>();
    Set<net.minecraft.resources.Identifier> attachmentTypeIds = new HashSet<>();

    for (var type : contentTypes) {
      if (!contentSpecs.add(type.spec()) || registry.get(type.spec()) != null) {
        throw new IllegalArgumentException("duplicate parcel content type: " + type.spec());
      }
    }

    for (var processor : processors) {
      if (!processorIds.add(processor.id())
          || ParcelRecordProcessorRegistry.get().get(processor.id()) != null) {
        throw new IllegalArgumentException("duplicate processor: " + processor.id());
      }
    }
    for (var type : attachmentTypes) {
      if (!attachmentTypeIds.add(type.id())
          || ParcelAttachmentTypeRegistry.get().get(type.id()) != null) {
        throw new IllegalArgumentException("duplicate attachment type: " + type.id());
      }
    }

    contentTypes.forEach(registry::register);
    processors.forEach(ParcelRecordProcessorRegistry.get()::register);
    attachmentTypes.forEach(ParcelAttachmentTypeRegistry.get()::register);
  }
}
