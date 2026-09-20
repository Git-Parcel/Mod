package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelExtent;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import io.github.leawind.gitparcel.common.testutils.TestNbt;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

/**
 * Tests the era-local form of lodestone compass NBT: 26.x runs the component shape, 1.20.1 runs
 * the legacy tag shape; each node executes its own branch of the helpers below.
 */
class LodestoneCompassProcessorTest extends AbstractMinecraftTest {
  private final LodestoneCompassProcessor processor = new LodestoneCompassProcessor();
  private final ParcelSpace space =
      new ParcelSpace(
          new ParcelTransform(Mirror.NONE, Rotation.CLOCKWISE_90, new BlockPos(100, 64, -40)));
  private final ParcelExtent extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));

  @Test
  void rebasesInsidePointingLodestonesOnCaptureAndRestore() throws ParcelException {
    BlockPos insideWorld = space.toWorld(new BlockPos(1, -1, 2));

    var captureRecord = chestWithCompassAt(insideWorld);
    var captured =
        processor.captureBlockEntity(captureContext(), captureRecord);
    assertEquals(
        space.toParcel(insideWorld), lodestoneOf(chestItem(captured.data())).orElseThrow());

    var restoreRecord = chestWithCompassAt(space.toParcel(insideWorld));
    var restored = processor.restoreBlockEntity(restoreContext(), restoreRecord);
    assertEquals(
        insideWorld, lodestoneOf(chestItem(restored.data())).orElseThrow());
  }

  /** Rule 3.2: lodestones outside the parcel keep their stored world value on both sides. */
  @Test
  void keepsOutsidePointingLodestonesIdentical() throws ParcelException {
    BlockPos outsideWorld = new BlockPos(1234, 64, -987);

    var captured =
        processor.captureBlockEntity(captureContext(), chestWithCompassAt(outsideWorld));
    assertEquals(outsideWorld, lodestoneOf(chestItem(captured.data())).orElseThrow());

    var restored =
        processor.restoreBlockEntity(restoreContext(), chestWithCompassAt(outsideWorld));
    assertEquals(outsideWorld, lodestoneOf(chestItem(restored.data())).orElseThrow());
  }

  /** Compasses without a tracker and non-compass items stay untouched. */
  @Test
  void ignoresPlainCompassesAndOtherItems() throws ParcelException {
    var data = new CompoundTag();
    var items = new ListTag();
    var plain = new CompoundTag();
    plain.putString("id", "minecraft:compass");
    items.add(plain);
    var unrelated = new CompoundTag();
    unrelated.putString("id", "minecraft:stone");
    items.add(unrelated);
    data.put("Items", items);

    var restored = processor.restoreBlockEntity(restoreContext(), new BlockEntityRecord(new BlockPos(0, 64, 0), data, List.of()));

    var restoredItems = TestNbt.getList(restored.data(), "Items").orElseThrow();
    assertEquals(
        "minecraft:compass",
        TestNbt.getString(TestNbt.getCompound(restoredItems, 0).orElseThrow(), "id").orElseThrow());
    assertEquals(
        "minecraft:stone",
        TestNbt.getString(TestNbt.getCompound(restoredItems, 1).orElseThrow(), "id").orElseThrow());
  }

  private ParcelRecordProcessorContext captureContext() {
    var session = new ParcelAttachmentSession();
    return new ParcelRecordProcessorContext(space, session, session, null, extent, 0L);
  }

  private ParcelRecordProcessorContext restoreContext() {
    return new ParcelRecordProcessorContext(
        space, new ParcelAttachmentSession(), null, null, extent, 0L);
  }

  private static BlockEntityRecord chestWithCompassAt(BlockPos pos) {
    var data = new CompoundTag();
    var items = new ListTag();
    items.add(compassAt(pos));
    data.put("Items", items);
    return new BlockEntityRecord(new BlockPos(0, 64, 0), data, List.of());
  }

  private static CompoundTag chestItem(CompoundTag blockEntityData) {
    return TestNbt.getCompound(
            TestNbt.getList(blockEntityData, "Items").orElseThrow(), 0)
        .orElseThrow();
  }

  private static CompoundTag compassAt(BlockPos pos) {
    var item = new CompoundTag();
    item.putString("id", "minecraft:compass");
    /*? if >=26.1 {*/
    var target = new CompoundTag();
    target.put("pos", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().orElseThrow());
    target.putString("dimension", "minecraft:overworld");
    var tracker = new CompoundTag();
    tracker.put("target", target);
    var components = new CompoundTag();
    components.put("minecraft:lodestone_tracker", tracker);
    item.put("components", components);
    return item;
    /*?} else {*/
    /*var tag = new CompoundTag();
    var stored = new CompoundTag();
    stored.putInt("X", pos.getX());
    stored.putInt("Y", pos.getY());
    stored.putInt("Z", pos.getZ());
    tag.put("LodestonePos", stored);
    tag.putString("LodestoneDimension", "minecraft:overworld");
    tag.putBoolean("LodestoneTracked", true);
    item.put("tag", tag);
    return item;
    *//*?}*/
  }

  private static java.util.Optional<BlockPos> lodestoneOf(CompoundTag item) {
    /*? if >=26.1 {*/
    var tracker =
        TestNbt.getCompound(
            TestNbt.getCompound(item, "components").orElseThrow(),
            "minecraft:lodestone_tracker");
    if (tracker.isEmpty()) {
      return java.util.Optional.empty();
    }
    return TestNbt.getCompound(tracker.orElseThrow(), "target")
        .flatMap(target -> NbtReads.read(target, "pos", BlockPos.CODEC));
    /*?} else {*/
    /*var tag = TestNbt.getCompound(item, "tag");
    if (tag.isEmpty()) {
      return java.util.Optional.empty();
    }
    var stored = TestNbt.getCompound(tag.orElseThrow(), "LodestonePos");
    if (stored.isEmpty()) {
      return java.util.Optional.empty();
    }
    var x = TestNbt.getInt(stored.orElseThrow(), "X");
    var y = TestNbt.getInt(stored.orElseThrow(), "Y");
    var z = TestNbt.getInt(stored.orElseThrow(), "Z");
    if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(new BlockPos(x.orElseThrow(), y.orElseThrow(), z.orElseThrow()));
    *//*?}*/
  }
}
