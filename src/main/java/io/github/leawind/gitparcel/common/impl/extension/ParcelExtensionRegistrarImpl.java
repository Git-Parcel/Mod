package io.github.leawind.gitparcel.common.impl.extension;

import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.RegistrationSource;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributor;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributorRegistry;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientFieldRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentTypeRegistry;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

final class ParcelExtensionRegistrarImpl implements ParcelExtensionRegistrar {
  private final GitParcelExtension extension;
  private final List<ParcelContentType<?>> contentTypes = new ArrayList<>();
  private final List<ParcelRecordProcessor> processors = new ArrayList<>();
  private final List<ParcelAttachmentType> attachmentTypes = new ArrayList<>();
  private final List<ParcelCoordinateField> coordinateFields = new ArrayList<>();
  private final List<ParcelEntityRefField> entityRefFields = new ArrayList<>();
  private final List<ParcelCaptureContributor> contributors = new ArrayList<>();
  private final List<ParcelTransientField> transientFields = new ArrayList<>();

  ParcelExtensionRegistrarImpl(GitParcelExtension extension) {
    this.extension = extension;
  }

  /**
   * Builds the rule 7.4 adjudication source for one entry: the extension is the entry's owner
   * exactly when it declares ownership of the entry's namespace. Content type ids are plain
   * directory names without a namespace, so those are never owned.
   */
  private RegistrationSource source(@Nullable String entryNamespace, int priority) {
    boolean owner =
        entryNamespace != null && extension.ownedNamespaces().contains(entryNamespace);
    return new RegistrationSource(extension.id(), owner, priority);
  }

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

  @Override
  public void registerCoordinateField(ParcelCoordinateField field) {
    coordinateFields.add(field);
  }

  @Override
  public void registerEntityRefField(ParcelEntityRefField field) {
    entityRefFields.add(field);
  }

  @Override
  public void registerContributor(ParcelCaptureContributor contributor) {
    contributors.add(contributor);
  }

  @Override
  public void registerTransientField(ParcelTransientField field) {
    transientFields.add(field);
  }

  void commit(ParcelContentTypeRegistry registry) {
    contentTypes.forEach(
        type -> registry.register(source(null, type.registrationPriority()), type));
    processors.forEach(
        processor ->
            ParcelRecordProcessorRegistry.get()
                .register(
                    source(processor.id().getNamespace(), processor.registrationPriority()),
                    processor));
    attachmentTypes.forEach(
        type ->
            ParcelAttachmentTypeRegistry.get()
                .register(source(type.id().getNamespace(), type.registrationPriority()), type));
    coordinateFields.forEach(ParcelCoordinateFieldRegistry.get()::register);
    entityRefFields.forEach(ParcelEntityRefFieldRegistry.get()::register);
    contributors.forEach(ParcelCaptureContributorRegistry.get()::register);
    transientFields.forEach(ParcelTransientFieldRegistry.get()::register);
  }
}
