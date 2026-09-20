package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import io.github.leawind.gitparcel.common.testutils.TestNbt;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MinecraftCoreRecordProcessorTest extends AbstractMinecraftTest {
  private final MinecraftCoreRecordProcessor processor = new MinecraftCoreRecordProcessor();
  private final ParcelSpace space =
      new ParcelSpace(
          new ParcelTransform(
              Mirror.FRONT_BACK,
              Rotation.CLOCKWISE_90,
              // The translation is the anchor's world position: the image of local (7, 3, -2).
              new ParcelTransform(
                      Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(100, 20, -40))
                  .apply(new BlockPos(7, 3, -2))));
  private final ParcelRecordProcessorContext context =
      new ParcelRecordProcessorContext(space, new ParcelAttachmentSession(), null, null, null, 0L);

  @Test
  void restoresBlockEntityCoordinatesFromAnchorRelativePosition() {
    var data = new CompoundTag();
    data.putInt("x", 999);
    data.putInt("y", 999);
    data.putInt("z", 999);
    var relative = new BlockPos(2, -1, 4);

    var restored =
        processor.restoreBlockEntity(
            context, new BlockEntityRecord(relative, data, List.of()));
    var world = space.toWorld(relative);

    assertEquals(world.getX(), TestNbt.getInt(restored.data(), "x").orElseThrow());
    assertEquals(world.getY(), TestNbt.getInt(restored.data(), "y").orElseThrow());
    assertEquals(world.getZ(), TestNbt.getInt(restored.data(), "z").orElseThrow());
  }

  @Test
  void restoresEntityPositionMotionYawAndRemovesUuid() {
    var relativePos = new Vec3(1.5, 2.25, -3.75);
    var localMotion = new Vec3(0.25, -0.5, 1.5);
    var data = new CompoundTag();
    data.put("Pos", doubles(relativePos));
    data.put("Motion", doubles(localMotion));
    data.put("Rotation", floats(37.5F, -12F));
    data.putString("UUID", "must-not-be-reused");
    var passengerPos = new Vec3(-4.25, 1.5, 2.75);
    var passenger = new CompoundTag();
    passenger.put("Pos", doubles(passengerPos));
    passenger.put("Motion", doubles(new Vec3(-1, 0.25, 0.5)));
    passenger.put("Rotation", floats(-80F, 5F));
    passenger.putString("UUID", "passenger-uuid");
    var passengers = new ListTag();
    passengers.add(passenger);
    data.put("Passengers", passengers);

    var restored =
        processor.restoreEntity(
            context,
            new EntityRecord(
                Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
                relativePos,
                BlockPos.containing(relativePos),
                data,
                List.of()));

    assertVecEquals(space.toWorld(relativePos), readVec(restored.data(), "Pos"));
    assertVecEquals(space.toWorldVector(localMotion), readVec(restored.data(), "Motion"));
    assertEquals(
        space.toWorldYaw(37.5F),
        TestNbt.getFloat(
                TestNbt.getList(restored.data(), "Rotation").orElseThrow(), 0)
            .orElseThrow(),
        1.0E-5F);
    assertEquals(
        -12F,
        TestNbt.getFloat(
                TestNbt.getList(restored.data(), "Rotation").orElseThrow(), 1)
            .orElseThrow());
    assertFalse(restored.data().contains("UUID"));
    assertFalse(restored.data().contains("block_pos"));
    var restoredPassenger =
        TestNbt.getCompound(TestNbt.getList(restored.data(), "Passengers").orElseThrow(), 0)
            .orElseThrow();
    assertVecEquals(space.toWorld(passengerPos), readVec(restoredPassenger, "Pos"));
    assertFalse(restoredPassenger.contains("UUID"));
  }

  @Test
  void capturesBlockEntityRootCoordinatesAsAnchorRelative() {
    var data = new CompoundTag();
    data.putInt("x", 999);
    data.putInt("y", 999);
    data.putInt("z", 999);
    var relative = new BlockPos(2, -1, 4);

    var captured =
        processor.captureBlockEntity(context, new BlockEntityRecord(relative, data, List.of()));

    assertEquals(relative.getX(), TestNbt.getInt(captured.data(), "x").orElseThrow());
    assertEquals(relative.getY(), TestNbt.getInt(captured.data(), "y").orElseThrow());
    assertEquals(relative.getZ(), TestNbt.getInt(captured.data(), "z").orElseThrow());
  }

  /**
   * The capture side works purely from the vanilla NBT tree: world-space Pos, Motion and Rotation
   * (as written by {@code Entity#save}) are rebased without touching a live entity.
   */
  @Test
  void capturesEntitySpatialFieldsFromTheNbtTree() {
    var relativePos = new Vec3(1.5, 2.25, -3.75);
    var worldPos = space.toWorld(relativePos);
    var worldMotion = new Vec3(0.25, -0.5, 1.5);
    var worldYaw = 37.5F;
    var data = new CompoundTag();
    data.put("Pos", doubles(worldPos));
    data.put("Motion", doubles(worldMotion));
    data.put("Rotation", floats(worldYaw, -12F));
    var passengerWorldPos = worldPos.add(1, 0, 0);
    var passenger = new CompoundTag();
    passenger.put("Pos", doubles(passengerWorldPos));
    passenger.put("Motion", doubles(new Vec3(-1, 0.25, 0.5)));
    passenger.put("Rotation", floats(-80F, 5F));
    var passengers = new ListTag();
    passengers.add(passenger);
    data.put("Passengers", passengers);
    var record =
        new EntityRecord(
            Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
            relativePos,
            BlockPos.containing(relativePos),
            data,
            List.of());

    var captured = processor.captureEntity(context, record);
    var restored = processor.restoreEntity(context, captured);

    assertVecEquals(worldPos, readVec(restored.data(), "Pos"));
    assertVecEquals(worldMotion, readVec(restored.data(), "Motion"));
    assertEquals(
        worldYaw,
        TestNbt.getFloat(
                TestNbt.getList(restored.data(), "Rotation").orElseThrow(), 0)
            .orElseThrow(),
        1.0E-5F);
    var restoredPassenger =
        TestNbt.getCompound(TestNbt.getList(restored.data(), "Passengers").orElseThrow(), 0)
            .orElseThrow();
    assertVecEquals(passengerWorldPos, readVec(restoredPassenger, "Pos"));
    assertVecEquals(new Vec3(-1, 0.25, 0.5), readVec(restoredPassenger, "Motion"));
  }

  @Test
  void restoresEntitySpatialFieldsForEveryTransform() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var placement = new ParcelTransform(mirror, rotation, new BlockPos(-30, 64, 11));
        // Fold the old anchor offset into the translation so it becomes the anchor's position.
        var space = new ParcelSpace(
            new ParcelTransform(mirror, rotation, placement.apply(new BlockPos(5, -2, 9))));
        var context = new ParcelRecordProcessorContext(space, new ParcelAttachmentSession(), null, null, null, 0L);
        var relativePos = new Vec3(1.5, 2.25, -3.75);
        var localMotion = new Vec3(0.25, -0.5, 1.5);
        var relativeBlockPos = new BlockPos(1, -1, 2);
        var data = new CompoundTag();
        data.put("Pos", doubles(relativePos));
        data.put("Motion", doubles(localMotion));
        data.put("Rotation", floats(37.5F, -12F));
        data.put("block_pos", blockPosList(relativeBlockPos));

        var restored =
            processor.restoreEntity(
                context,
                new EntityRecord(
                    Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
                    relativePos,
                    relativeBlockPos,
                    data,
                    List.of()));

        assertVecEquals(space.toWorld(relativePos), readVec(restored.data(), "Pos"));
        assertVecEquals(space.toWorldVector(localMotion), readVec(restored.data(), "Motion"));
        assertEquals(
            space.toWorldYaw(37.5F),
            TestNbt.getFloat(TestNbt.getList(restored.data(), "Rotation").orElseThrow(), 0)
                .orElseThrow(),
            1.0E-5F,
            "yaw for mirror=%s rotation=%s".formatted(mirror, rotation));
        assertEquals(
            space.toWorld(relativeBlockPos),
            NbtReads.read(restored.data(), "block_pos", BlockPos.CODEC)
                .orElseThrow(() -> new AssertionError("block_pos lost for " + rotation)));
      }
    }
  }

  /**
   * Documents the core processor's scope: only whitelisted spatial fields are rebased. Nested
   * fields inside entity or block-entity NBT are the declared-field processor's job; keys without
   * a declaration travel verbatim.
   */
  @Test
  void leavesNestedPositionFieldsUntouched() {
    var customPos = blockPosList(new BlockPos(120, 64, -35));
    var beData = new CompoundTag();
    beData.putInt("x", 5);
    beData.putInt("y", 64);
    beData.putInt("z", -30);
    beData.put("custom_pos", customPos);

    var restoredBe =
        processor.restoreBlockEntity(
            context, new BlockEntityRecord(new BlockPos(1, 2, 3), beData, List.of()));

    assertEquals(
        customPos,
        TestNbt.getList(restoredBe.data(), "custom_pos").orElseThrow(),
        "undeclared nested fields must travel verbatim");

    var leashPos = new CompoundTag();
    leashPos.putInt("X", 120);
    leashPos.putInt("Y", 64);
    leashPos.putInt("Z", -35);
    var entityData = new CompoundTag();
    entityData.put("Pos", doubles(new Vec3(1, 1, 1)));
    entityData.put("leash", leashPos);

    var restoredEntity =
        processor.restoreEntity(
            context,
            new EntityRecord(
                Identifier.fromNamespaceAndPath("minecraft", "cow"),
                new Vec3(1, 1, 1),
                new BlockPos(1, 1, 1),
                entityData,
                List.of()));

    assertEquals(
        leashPos,
        TestNbt.getCompound(restoredEntity.data(), "leash").orElseThrow(),
        "leash position variant must travel verbatim");
  }

  private static ListTag blockPosList(BlockPos pos) {
    var list = new ListTag();
    list.add(net.minecraft.nbt.IntTag.valueOf(pos.getX()));
    list.add(net.minecraft.nbt.IntTag.valueOf(pos.getY()));
    list.add(net.minecraft.nbt.IntTag.valueOf(pos.getZ()));
    return list;
  }

  private static ListTag doubles(Vec3 value) {
    var list = new ListTag();
    list.add(DoubleTag.valueOf(value.x));
    list.add(DoubleTag.valueOf(value.y));
    list.add(DoubleTag.valueOf(value.z));
    return list;
  }

  private static ListTag floats(float yaw, float pitch) {
    var list = new ListTag();
    list.add(FloatTag.valueOf(yaw));
    list.add(FloatTag.valueOf(pitch));
    return list;
  }

  private static Vec3 readVec(CompoundTag data, String key) {
    var list = TestNbt.getList(data, key).orElseThrow();
    return new Vec3(
        TestNbt.getDouble(list, 0).orElseThrow(),
        TestNbt.getDouble(list, 1).orElseThrow(),
        TestNbt.getDouble(list, 2).orElseThrow());
  }

  private static void assertVecEquals(Vec3 expected, Vec3 actual) {
    assertEquals(expected.x, actual.x, 1.0E-9);
    assertEquals(expected.y, actual.y, 1.0E-9);
    assertEquals(expected.z, actual.z, 1.0E-9);
  }
}
