package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The self-description a snapshot carries (SEMANTICS.md rule 7.2): the processors, declared
 * fields, and attachment types that were effective when the snapshot was captured.
 *
 * <p>Restore adjudicates participation against this manifest instead of the current registries
 * (rule 7.3): processors and fields absent from the manifest do not run, so a snapshot whose
 * spatial fields were never relativized is never misinterpreted by a processor registered later.
 * The manifest is written into {@code parcel.json} on every capture; a snapshot without one is
 * interpreted conservatively (only the always-on core processor participates).
 */
public record ParcelSemantics(
    List<Identifier> processors,
    List<CoordinateField> coordinateFields,
    List<ReferenceField> entityRefFields,
    List<AttachmentType> attachments) {
  public static final Codec<ParcelSemantics> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Identifier.CODEC.listOf()
                          .fieldOf("processors")
                          .forGetter(ParcelSemantics::processors),
                      CoordinateField.CODEC
                          .listOf()
                          .fieldOf("coordinate_fields")
                          .forGetter(ParcelSemantics::coordinateFields),
                      ReferenceField.CODEC
                          .listOf()
                          .fieldOf("entity_ref_fields")
                          .forGetter(ParcelSemantics::entityRefFields),
                      AttachmentType.CODEC
                          .listOf()
                          .fieldOf("attachments")
                          .forGetter(ParcelSemantics::attachments))
                  .apply(inst, ParcelSemantics::new));

  public ParcelSemantics {
    processors = List.copyOf(processors);
    coordinateFields = List.copyOf(coordinateFields);
    entityRefFields = List.copyOf(entityRefFields);
    attachments = List.copyOf(attachments);
  }

  /** Records every processor, declared field, and attachment type currently registered. */
  public static ParcelSemantics captureCurrent() {
    var processors =
        ParcelRecordProcessorRegistry.get().orderedProcessors().stream()
            .map(ParcelRecordProcessor::id)
            .toList();
    var coordinateFields =
        ParcelCoordinateFieldRegistry.get().fields().stream()
            .map(CoordinateField::of)
            .toList();
    var entityRefFields =
        ParcelEntityRefFieldRegistry.get().fields().stream()
            .map(ReferenceField::of)
            .toList();
    var attachments =
        ParcelAttachmentTypeRegistry.get().types().stream()
            .map(AttachmentType::of)
            .toList();
    return new ParcelSemantics(processors, coordinateFields, entityRefFields, attachments);
  }

  public boolean declaresProcessor(Identifier id) {
    return processors.contains(id);
  }

  public boolean declares(ParcelCoordinateField field) {
    return coordinateFields.stream().anyMatch(recorded -> recorded.matches(field));
  }

  public boolean declares(ParcelEntityRefField field) {
    return entityRefFields.stream().anyMatch(recorded -> recorded.matches(field));
  }

  /**
   * One declared world-position or orientation field. The type is {@code null} when the
   * declaration applies to every entity or block-entity type.
   */
  public record CoordinateField(
      String target,
      @Nullable String type,
      String path,
      String encoding,
      @Nullable String pointing) {
    public static final Codec<CoordinateField> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.fieldOf("target").forGetter(CoordinateField::target),
                        Codec.STRING
                            .optionalFieldOf("type")
                            .forGetter(field -> Optional.ofNullable(field.type())),
                        Codec.STRING.fieldOf("path").forGetter(CoordinateField::path),
                        Codec.STRING.fieldOf("encoding").forGetter(CoordinateField::encoding),
                        Codec.STRING
                            .optionalFieldOf("pointing")
                            .forGetter(field -> Optional.ofNullable(field.pointing())))
                    .apply(
                        inst,
                        (target, type, path, encoding, pointing) ->
                            new CoordinateField(
                                target, type.orElse(null), path, encoding, pointing.orElse(null))));

    public static CoordinateField of(ParcelCoordinateField field) {
      return new CoordinateField(
          field.target().name(),
          field.type().map(Identifier::toString).orElse(null),
          field.path(),
          field.encoding().name(),
          field.pointing() == ParcelCoordinateField.Pointing.GEOMETRIC
              ? null
              : field.pointing().name());
    }

    public boolean matches(ParcelCoordinateField field) {
      return target.equals(field.target().name())
          && path.equals(field.path())
          && encoding.equals(field.encoding().name())
          && java.util.Objects.equals(type, field.type().map(Identifier::toString).orElse(null))
          && java.util.Objects.equals(pointing, recordedPointing(field));
    }

    private static @Nullable String recordedPointing(ParcelCoordinateField field) {
      return field.pointing() == ParcelCoordinateField.Pointing.GEOMETRIC
          ? null
          : field.pointing().name();
    }
  }

  /** One declared entity-UUID reference field; {@code null} type applies to every entity. */
  public record ReferenceField(@Nullable String type, String path) {
    public static final Codec<ReferenceField> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING
                            .optionalFieldOf("type")
                            .forGetter(field -> Optional.ofNullable(field.type())),
                        Codec.STRING.fieldOf("path").forGetter(ReferenceField::path))
                    .apply(
                        inst,
                        (type, path) -> new ReferenceField(type.orElse(null), path)));

    public static ReferenceField of(ParcelEntityRefField field) {
      return new ReferenceField(
          field.type().map(Identifier::toString).orElse(null), field.path());
    }

    public boolean matches(ParcelEntityRefField field) {
      return path.equals(field.path())
          && java.util.Objects.equals(type, field.type().map(Identifier::toString).orElse(null));
    }
  }

  /** One attachment type recorded with the schema version effective at capture time. */
  public record AttachmentType(Identifier type, int schemaVersion) {
    public static final Codec<AttachmentType> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Identifier.CODEC.fieldOf("type").forGetter(AttachmentType::type),
                        Codec.INT.fieldOf("schema_version").forGetter(AttachmentType::schemaVersion))
                    .apply(inst, AttachmentType::new));

    public static AttachmentType of(
        io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType type) {
      return new AttachmentType(type.id(), type.schemaVersion());
    }
  }
}
