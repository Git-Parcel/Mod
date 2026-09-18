package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PaintingRecordProcessorTest extends AbstractMinecraftTest {
  @Test
  void restoresParcelRelativeFacing() throws Exception {
    var space =
        new ParcelSpace(
            new ParcelTransform(
                Mirror.LEFT_RIGHT,
                Rotation.CLOCKWISE_90,
                // The translation is the anchor's world position: the image of local (3, 1, 7).
                new ParcelTransform(
                        Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, new BlockPos(20, 4, -8))
                    .apply(new BlockPos(3, 1, 7))));
    var payload = new CompoundTag();
    payload.putString("direction", Direction.WEST.getName());
    var record = record(new SemanticData(PaintingRecordProcessor.ID, 0, payload));

    var restored =
        new PaintingRecordProcessor()
            .restoreEntity(
                new ParcelRecordProcessorContext(
                    space, new ParcelAttachmentSession(), null, null, null, 0L),
                record);

    assertEquals(
        space.toWorldDirection(Direction.WEST).get2DDataValue(),
        restored.data().getInt("facing").orElseThrow());
  }

  /**
   * Capture re-derives the wall direction from the vanilla {@code facing} byte (legacy 2D data
   * value) instead of the live entity, and stores it parcel-relative in the semantic payload.
   */
  @Test
  void capturesFacingFromTheVanillaNbtByte() {
    var space = ParcelSpaceTestValues.IDENTITY;
    var data = new CompoundTag();
    data.putInt("facing", Direction.NORTH.get2DDataValue());
    var record = record(data, null);

    var captured =
        new PaintingRecordProcessor()
            .captureEntity(
                new ParcelRecordProcessorContext(
                    space, new ParcelAttachmentSession(), new ParcelAttachmentSession(), null, null, 0L),
                record);

    var semantic = captured.semanticData().get(0);
    assertEquals(
        Direction.NORTH.getName(),
        semantic.payload().getString("direction").orElseThrow());
  }

  /** Records of other entity types pass through untouched. */
  @Test
  void ignoresOtherEntityTypes() {
    var data = new CompoundTag();
    data.putInt("facing", Direction.NORTH.get2DDataValue());
    var record =
        new EntityRecord(
            Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
            Vec3.ZERO,
            BlockPos.ZERO,
            data,
            List.of());

    var captured =
        new PaintingRecordProcessor()
            .captureEntity(
                new ParcelRecordProcessorContext(
                    ParcelSpaceTestValues.IDENTITY,
                    new ParcelAttachmentSession(),
                    new ParcelAttachmentSession(),
                    null,
                    null,
                    0L),
                record);

    assertTrue(captured.semanticData().isEmpty());
  }

  @Test
  void rejectsUnknownSemanticSchema() {
    var payload = new CompoundTag();
    payload.putString("direction", Direction.NORTH.getName());

    assertThrows(
        ParcelException.class,
        () ->
            new PaintingRecordProcessor()
                .restoreEntity(
                    new ParcelRecordProcessorContext(
                        ParcelSpaceTestValues.IDENTITY,
                        new ParcelAttachmentSession(),
                        null,
                        null,
                        null,
                        0L),
                    record(new CompoundTag(), new SemanticData(PaintingRecordProcessor.ID, 99, payload))));
  }

  private static EntityRecord record(CompoundTag data, SemanticData semantic) {
    return new EntityRecord(
        Identifier.fromNamespaceAndPath("minecraft", "painting"),
        Vec3.ZERO,
        BlockPos.ZERO,
        data,
        semantic == null ? List.of() : List.of(semantic));
  }

  private static EntityRecord record(SemanticData semantic) {
    return record(new CompoundTag(), semantic);
  }

  private static final class ParcelSpaceTestValues {
    private static final ParcelSpace IDENTITY = new ParcelSpace(ParcelTransform.IDENTITY);
  }
}
