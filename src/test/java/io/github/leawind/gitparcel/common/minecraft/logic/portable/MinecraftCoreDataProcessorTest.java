package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
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

class MinecraftCoreDataProcessorTest extends AbstractMinecraftTest {
  private final MinecraftCoreDataProcessor processor = new MinecraftCoreDataProcessor();
  private final ParcelSpace space =
      new ParcelSpace(
          new ParcelTransform(
              Mirror.FRONT_BACK,
              Rotation.CLOCKWISE_90,
              new BlockPos(100, 20, -40)),
          new BlockPos(7, 3, -2));
  private final ParcelProcessorContext context =
      new ParcelProcessorContext(null, space, new ParcelAttachmentSession());

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

    assertEquals(world.getX(), restored.data().getInt("x").orElseThrow());
    assertEquals(world.getY(), restored.data().getInt("y").orElseThrow());
    assertEquals(world.getZ(), restored.data().getInt("z").orElseThrow());
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
        restored
            .data()
            .getList("Rotation")
            .orElseThrow()
            .getFloat(0)
            .orElseThrow(),
        1.0E-5F);
    assertEquals(
        -12F,
        restored
            .data()
            .getList("Rotation")
            .orElseThrow()
            .getFloat(1)
            .orElseThrow());
    assertFalse(restored.data().contains("UUID"));
    assertFalse(restored.data().contains("block_pos"));
    var restoredPassenger =
        restored
            .data()
            .getList("Passengers")
            .orElseThrow()
            .getCompound(0)
            .orElseThrow();
    assertVecEquals(space.toWorld(passengerPos), readVec(restoredPassenger, "Pos"));
    assertFalse(restoredPassenger.contains("UUID"));
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
    var list = data.getList(key).orElseThrow();
    return new Vec3(
        list.getDouble(0).orElseThrow(),
        list.getDouble(1).orElseThrow(),
        list.getDouble(2).orElseThrow());
  }

  private static void assertVecEquals(Vec3 expected, Vec3 actual) {
    assertEquals(expected.x, actual.x, 1.0E-9);
    assertEquals(expected.y, actual.y, 1.0E-9);
    assertEquals(expected.z, actual.z, 1.0E-9);
  }
}
