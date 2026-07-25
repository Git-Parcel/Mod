package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelProcessorContext;
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

class PaintingDataProcessorTest extends AbstractMinecraftTest {
  @Test
  void restoresParcelRelativeFacing() throws Exception {
    var space =
        new ParcelSpace(
            new ParcelTransform(
                Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, new BlockPos(20, 4, -8)),
            new BlockPos(3, 1, 7));
    var payload = new CompoundTag();
    payload.putString("direction", Direction.WEST.getName());
    var record = record(new SemanticData(PaintingDataProcessor.ID, 0, payload));

    var restored =
        new PaintingDataProcessor()
            .restoreEntity(
                new ParcelProcessorContext(null, space, new ParcelAttachmentSession()),
                record);

    assertEquals(
        space.toWorldDirection(Direction.WEST).get2DDataValue(),
        restored.data().getInt("facing").orElseThrow());
  }

  @Test
  void rejectsUnknownSemanticSchema() {
    var payload = new CompoundTag();
    payload.putString("direction", Direction.NORTH.getName());

    assertThrows(
        ParcelException.class,
        () ->
            new PaintingDataProcessor()
                .restoreEntity(
                    new ParcelProcessorContext(
                        null, ParcelSpaceTestValues.IDENTITY, new ParcelAttachmentSession()),
                    record(new SemanticData(PaintingDataProcessor.ID, 99, payload))));
  }

  private static EntityRecord record(SemanticData semantic) {
    return new EntityRecord(
        Identifier.fromNamespaceAndPath("minecraft", "painting"),
        Vec3.ZERO,
        BlockPos.ZERO,
        new CompoundTag(),
        List.of(semantic));
  }

  private static final class ParcelSpaceTestValues {
    private static final ParcelSpace IDENTITY =
        new ParcelSpace(ParcelTransform.IDENTITY, BlockPos.ZERO);
  }
}
