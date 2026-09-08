package io.github.leawind.gitparcel.common.minecraft.logic.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pins the structural validation performed while decoding a portable snapshot. */
class ParcelStorageValidationTest extends AbstractMinecraftTest {
  private static final Identifier PROBE_ATTACHMENT_TYPE =
      Identifier.fromNamespaceAndPath("gitparceltest", "probe");
  private static final int PROBE_SCHEMA_VERSION = 1;

  /**
   * Parcel space spans parcel-relative x in [-2, 6), y in [-1, 7), z in [0, 8) with the anchor (2,
   * 1, 0) and size (8, 8, 8).
   */
  private final ParcelStorage.SnapshotValidationSink sink =
      new ParcelStorage.SnapshotValidationSink(
          new ParcelMeta(Map.of(), 3953, new Vec3i(8, 8, 8), new BlockPos(2, 1, 0)));

  @BeforeAll
  static void registerProbeAttachmentType() {
    var registry = ParcelAttachmentTypeRegistry.get();
    if (registry.get(PROBE_ATTACHMENT_TYPE) == null) {
      registry.register(probeAttachmentType());
    }
  }

  @Test
  void skipsOptionalAttachmentWithUnknownType() {
    assertDoesNotThrow(
        () ->
            sink.acceptAttachment(
                attachment(Identifier.fromNamespaceAndPath("gitparceltest", "unknown"), 1, false)));
  }

  @Test
  void rejectsRequiredAttachmentWithUnknownType() {
    assertThrows(
        ParcelException.class,
        () ->
            sink.acceptAttachment(
                attachment(Identifier.fromNamespaceAndPath("gitparceltest", "unknown"), 1, true)));
  }

  @Test
  void rejectsAttachmentWithWrongSchemaVersion() {
    assertThrows(
        ParcelException.class,
        () -> sink.acceptAttachment(attachment(PROBE_ATTACHMENT_TYPE, PROBE_SCHEMA_VERSION + 1, true)));
  }

  @Test
  void acceptsAttachmentWithMatchingTypeAndSchemaVersion() {
    assertDoesNotThrow(
        () -> sink.acceptAttachment(attachment(PROBE_ATTACHMENT_TYPE, PROBE_SCHEMA_VERSION, true)));
  }

  @Test
  void rejectsEntityOutsideParcelBounds() {
    assertThrows(
        ParcelException.class, () -> sink.acceptEntity(entity(new Vec3(6, 0, 1), new BlockPos(5, 0, 1))));
    assertThrows(
        ParcelException.class, () -> sink.acceptEntity(entity(new Vec3(-2.5, 0, 1), new BlockPos(-3, 0, 1))));
    assertThrows(ParcelException.class, () -> sink.acceptEntity(entity(new Vec3(Double.NaN, 0, 1), BlockPos.ZERO)));
  }

  @Test
  void acceptsEntityInsideParcelBounds() {
    assertDoesNotThrow(() -> sink.acceptEntity(entity(new Vec3(0, 0, 1), new BlockPos(0, 0, 1))));
  }

  @Test
  void rejectsBlockSectionOutsideParcelBounds() {
    assertThrows(
        ParcelException.class, () -> sink.acceptBlockSection(section(new BlockPos(6, 0, 0), List.of())));
    assertThrows(
        ParcelException.class, () -> sink.acceptBlockSection(section(new BlockPos(0, 0, 8), List.of())));
  }

  @Test
  void rejectsBlockEntityOutsideItsSection() {
    var sectionOrigin = new BlockPos(0, 0, 1);
    var inside = sectionOrigin;
    var outside = sectionOrigin.offset(4, 0, 0);

    assertThrows(
        ParcelException.class,
        () -> sink.acceptBlockSection(section(sectionOrigin, List.of(blockEntity(outside)))));
    assertDoesNotThrow(
        () -> sink.acceptBlockSection(section(sectionOrigin, List.of(blockEntity(inside)))));
  }

  @Test
  void rejectsSemanticDataWithUnknownProcessor() {
    var unknown =
        List.of(
            new SemanticData(
                Identifier.fromNamespaceAndPath("gitparceltest", "unknown-processor"), 1, new CompoundTag()));

    assertThrows(
        ParcelException.class,
        () ->
            sink.acceptEntity(
                new EntityRecord(
                    Identifier.fromNamespaceAndPath("minecraft", "cow"),
                    new Vec3(0, 0, 1),
                    new BlockPos(0, 0, 1),
                    new CompoundTag(),
                    unknown)));
  }

  private static ParcelAttachmentType probeAttachmentType() {
    return new ParcelAttachmentType() {
      @Override
      public Identifier id() {
        return PROBE_ATTACHMENT_TYPE;
      }

      @Override
      public int schemaVersion() {
        return PROBE_SCHEMA_VERSION;
      }

      @Override
      public void restore(
          ParcelRecordProcessorContext context, AttachmentRecord attachment) {}
    };
  }

  private static AttachmentRecord attachment(Identifier type, int schemaVersion, boolean required) {
    return new AttachmentRecord(
        new LocalAttachmentId("probe0001"), type, schemaVersion, required, new CompoundTag());
  }

  private static EntityRecord entity(Vec3 pos, BlockPos blockPos) {
    return new EntityRecord(
        Identifier.fromNamespaceAndPath("minecraft", "cow"),
        pos,
        blockPos,
        new CompoundTag(),
        List.of());
  }

  private static BlockEntityRecord blockEntity(BlockPos pos) {
    return new BlockEntityRecord(pos, new CompoundTag(), List.of());
  }

  private static BlockSection section(
      BlockPos origin, List<BlockEntityRecord> blockEntities) {
    return new BlockSection(
        origin,
        new Vec3i(1, 1, 1),
        List.of(Blocks.STONE.defaultBlockState()),
        blockEntities);
  }
}
