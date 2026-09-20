package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * The 26.x painting stores its facing as a legacy-id byte; capture must record the semantic data
 * and restore must rewrite the byte.
 */
class PaintingByteFacingTest extends AbstractMinecraftTest {
  private final PaintingRecordProcessor processor = new PaintingRecordProcessor();

  @Test
  void capturesByteFacingIntoSemanticDataAndRestoresIt()
      throws io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
    var identity =
        new ParcelSpace(new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(0, 0, 0)));
    var mirrored =
        new ParcelSpace(new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, new BlockPos(0, 0, 0)));
    var session = new ParcelAttachmentSession();
    var captureContext =
        new ParcelRecordProcessorContext(identity, session, session, null, null, 0L);

    var data = new CompoundTag();
    data.putString("id", "minecraft:painting");
    data.put("facing", ByteTag.valueOf((byte) Direction.SOUTH.get2DDataValue()));
    var record =
        new EntityRecord(
            Identifier.fromNamespaceAndPath("minecraft", "painting"),
            new Vec3(0, 0, 0),
            BlockPos.ZERO,
            data,
            List.of());

    var captured = processor.captureEntity(captureContext, record);
    assertFalse(
        captured.semanticData().isEmpty(), "capture must record the painting semantic payload");

    var restoredContext =
        new ParcelRecordProcessorContext(mirrored, new ParcelAttachmentSession(), null, null, null, 0L);
    var restored = processor.restoreEntity(restoredContext, captured);
    var facing = restored.data().get("facing");
    assertTrue(facing instanceof ByteTag, "facing must stay a byte");
    assertEquals(
        Direction.NORTH.get2DDataValue(),
        NbtReads.intValue((net.minecraft.nbt.NumericTag) facing));
  }
}
