package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DeclaredCoordinateFieldProcessorTest extends AbstractMinecraftTest {
  private static final Identifier TEST_ENTITY = id("entity");
  private static final Identifier TEST_OTHER_ENTITY = id("other_entity");
  private static final Identifier TEST_BLOCK_ENTITY = id("block_entity");

  private final DeclaredCoordinateFieldProcessor processor = new DeclaredCoordinateFieldProcessor();
  private final ParcelSpace space =
      new ParcelSpace(
          new ParcelTransform(
              Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(100, 20, -40)),
          new BlockPos(7, 3, -2));

  @BeforeAll
  static void registerFields() {
    var registry = ParcelCoordinateFieldRegistry.get();
    if (registry.fields().stream().anyMatch(field -> field.path().equals("home_xyz"))) {
      return;
    }
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY, TEST_ENTITY, "home", ParcelCoordinateField.Encoding.BLOCK_POS));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY, TEST_ENTITY, "home_xyz", ParcelCoordinateField.Encoding.BLOCK_POS_XYZ));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY, TEST_ENTITY, "sprint_target", ParcelCoordinateField.Encoding.POSITION));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.BLOCK_ENTITY,
            TEST_BLOCK_ENTITY,
            "Items[].tag.waypoint",
            ParcelCoordinateField.Encoding.POSITION));
  }

  @Test
  void restoresDeclaredEntityFieldsToParcelRelativeOnCapture() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(120, 70, -30));
    putBlockPosXyz(data, "home_xyz", new BlockPos(120, 70, -30));
    putPositionCodec(data, "sprint_target", new Vec3(120.5, 70.25, -30.5));
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var captured = processor.captureEntity(context(), null, record);

    assertEquals(
        space.toParcel(new BlockPos(120, 70, -30)),
        readBlockPosCodec(captured.data(), "home").orElseThrow());
    assertEquals(
        space.toParcel(new BlockPos(120, 70, -30)),
        readBlockPosXyz(captured.data(), "home_xyz").orElseThrow());
    assertEquals(
        space.toParcel(new Vec3(120.5, 70.25, -30.5)),
        readPositionCodec(captured.data(), "sprint_target").orElseThrow());
  }

  @Test
  void restoresDeclaredEntityFieldsToWorldSpace() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(4, -1, 2));
    putBlockPosXyz(data, "home_xyz", new BlockPos(4, -1, 2));
    putPositionCodec(data, "sprint_target", new Vec3(4.5, -1.25, 2.5));
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readBlockPosCodec(restored.data(), "home").orElseThrow());
    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readBlockPosXyz(restored.data(), "home_xyz").orElseThrow());
    assertEquals(
        space.toWorld(new Vec3(4.5, -1.25, 2.5)),
        readPositionCodec(restored.data(), "sprint_target").orElseThrow());
  }

  @Test
  void transformsDeclaredBlockEntityFieldsInsideItemStacks() {
    var data = new CompoundTag();
    data.putString("id", TEST_BLOCK_ENTITY.toString());
    var items = new ListTag();
    for (int i = 0; i < 2; i++) {
      var item = new CompoundTag();
      var tag = new CompoundTag();
      putPositionCodec(tag, "waypoint", new Vec3(10 + i, 64, -5));
      item.put("tag", tag);
      items.add(item);
    }
    data.put("Items", items);
    var record = new BlockEntityRecord(new BlockPos(0, 0, 0), data, List.of());

    var restored = processor.restoreBlockEntity(context(), record);

    var restoredItems = restored.data().getList("Items").orElseThrow();
    for (int i = 0; i < 2; i++) {
      assertEquals(
          space.toWorld(new Vec3(10 + i, 64, -5)),
          readPositionCodec(
                  restoredItems.getCompound(i).orElseThrow().getCompound("tag").orElseThrow(),
                  "waypoint")
              .orElseThrow());
    }
  }

  @Test
  void respectsTypeScopesAndUndeclaredTypes() {
    var data = entityData(TEST_OTHER_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(120, 70, -30));
    putBlockPosXyz(data, "home_xyz", new BlockPos(120, 70, -30));
    var record =
        new EntityRecord(TEST_OTHER_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    assertEquals(
        new BlockPos(120, 70, -30),
        readBlockPosCodec(restored.data(), "home").orElseThrow(),
        "fields declared for another entity type must not be touched");
    assertEquals(
        new BlockPos(120, 70, -30), readBlockPosXyz(restored.data(), "home_xyz").orElseThrow());
  }

  @Test
  void appliesDeclaredFieldsToPassengersFilteredByNestedType() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(4, -1, 2));
    var passengers = new ListTag();
    var sameType = entityData(TEST_ENTITY);
    putBlockPosCodec(sameType, "home", new BlockPos(4, -1, 2));
    var otherType = entityData(TEST_OTHER_ENTITY);
    putBlockPosCodec(otherType, "home", new BlockPos(120, 70, -30));
    passengers.add(sameType);
    passengers.add(otherType);
    data.put("Passengers", passengers);
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    var restoredPassengers = restored.data().getList("Passengers").orElseThrow();
    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readBlockPosCodec(restoredPassengers.getCompound(0).orElseThrow(), "home").orElseThrow());
    assertEquals(
        new BlockPos(120, 70, -30),
        readBlockPosCodec(restoredPassengers.getCompound(1).orElseThrow(), "home").orElseThrow());
  }

  @Test
  void roundTripsDeclaredFieldsAcrossEveryTransform() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var space = new ParcelSpace(new ParcelTransform(mirror, rotation, new BlockPos(-30, 64, 11)), new BlockPos(5, -2, 9));
        var context =
            new ParcelRecordProcessorContext(null, space, new ParcelAttachmentSession(), null);
        var world = new BlockPos(120, 70, -30);
        var data = entityData(TEST_ENTITY);
        putBlockPosCodec(data, "home", world);
        var record =
            new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

        var captured = processor.captureEntity(context, null, record);
        var restored = processor.restoreEntity(context, captured);
        var restoredPos =
            readBlockPosCodec(restored.data(), "home")
                .orElseThrow(
                    () -> new AssertionError("home lost for mirror=" + mirror + " rotation=" + rotation));

        assertEquals(
            world,
            restoredPos,
            "capture then restore must return the original world position for mirror=%s rotation=%s"
                .formatted(mirror, rotation));
      }
    }
  }

  @Test
  void leavesMissingPathsUntouched() {
    var data = entityData(TEST_ENTITY);
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    assertTrue(restored.data().getList("home").isEmpty());
  }

  private ParcelRecordProcessorContext context() {
    return new ParcelRecordProcessorContext(null, space, new ParcelAttachmentSession(), null);
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath("gitparceltest", path);
  }

  private static CompoundTag entityData(Identifier type) {
    var data = new CompoundTag();
    data.putString("id", type.toString());
    return data;
  }

  private static void putBlockPosCodec(CompoundTag data, String key, BlockPos pos) {
    data.put(key, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow());
  }

  private static java.util.Optional<BlockPos> readBlockPosCodec(CompoundTag data, String key) {
    var tag = data.get(key);
    return tag == null ? java.util.Optional.empty() : BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }

  private static void putBlockPosXyz(CompoundTag data, String key, BlockPos pos) {
    var compound = new CompoundTag();
    compound.putInt("X", pos.getX());
    compound.putInt("Y", pos.getY());
    compound.putInt("Z", pos.getZ());
    data.put(key, compound);
  }

  private static java.util.Optional<BlockPos> readBlockPosXyz(CompoundTag data, String key) {
    return data
        .getCompound(key)
        .map(
            compound ->
                new BlockPos(
                    compound.getInt("X").orElseThrow(),
                    compound.getInt("Y").orElseThrow(),
                    compound.getInt("Z").orElseThrow()));
  }

  private static void putPositionCodec(CompoundTag data, String key, Vec3 pos) {
    data.put(key, Vec3.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow());
  }

  private static java.util.Optional<Vec3> readPositionCodec(CompoundTag data, String key) {
    var tag = data.get(key);
    return tag == null ? java.util.Optional.empty() : Vec3.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }
}
