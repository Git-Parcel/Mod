package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Item frames serialize Facing and ItemRotation as legacy-id bytes on both eras; the rotation
 * step must rebase against the byte-decoded facing exactly as it does for the string form.
 */
class ItemFrameRotationByteTest extends AbstractMinecraftTest {
  private static final Identifier FRAME = Identifier.fromNamespaceAndPath("minecraft", "item_frame");

  private final DeclaredCoordinateFieldProcessor processor = new DeclaredCoordinateFieldProcessor();

  @BeforeAll
  static void registerFields() {
    var registry = ParcelCoordinateFieldRegistry.get();
    if (registry.fields().stream()
        .anyMatch(field -> field.path().equals("ItemRotation") && field.type().isPresent())) {
      return;
    }
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            FRAME,
            "Facing",
            ParcelCoordinateField.Encoding.DIRECTION));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            FRAME,
            "ItemRotation",
            ParcelCoordinateField.Encoding.ROTATION_STEP));
  }

  @Test
  void mirroredFrameRotationNegatesThroughByteFacing() {
    var identity =
        new ParcelSpace(new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(0, 0, 0)));
    var mirrored =
        new ParcelSpace(new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, new BlockPos(0, 0, 0)));
    var captureSession = new ParcelAttachmentSession();
    var captureContext =
        new ParcelRecordProcessorContext(identity, captureSession, captureSession, null, null, 0L);

    var data = new CompoundTag();
    data.putString("id", "minecraft:item_frame");
    data.put("Facing", ByteTag.valueOf((byte) Direction.SOUTH.get3DDataValue()));
    data.put("ItemRotation", ByteTag.valueOf((byte) 3));
    var record = new EntityRecord(FRAME, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var captured = processor.captureEntity(captureContext, record);
    int capturedStep = NbtReads.intValue((net.minecraft.nbt.NumericTag) captured.data().get("ItemRotation"));

    var restoreContext =
        new ParcelRecordProcessorContext(
            mirrored, new ParcelAttachmentSession(), null, fullSemantics(), null, 0L);
    var restored = processor.restoreEntity(restoreContext, captured);
    int restoredStep = NbtReads.intValue((net.minecraft.nbt.NumericTag) restored.data().get("ItemRotation"));

    assertEquals(5, restoredStep, "mirror must negate the 3-step rotation (3 -> 5)");
  }

  private static ParcelSemantics fullSemantics() {
    return new ParcelSemantics(
        List.of(),
        ParcelCoordinateFieldRegistry.get().fields().stream()
            .filter(field -> field.type().isPresent() && field.type().orElseThrow().equals(FRAME))
            .map(ParcelSemantics.CoordinateField::of)
            .toList(),
        List.of(),
        List.of());
  }
}
