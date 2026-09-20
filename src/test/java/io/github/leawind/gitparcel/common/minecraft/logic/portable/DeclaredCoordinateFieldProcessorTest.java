package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.ParcelExtent;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import io.github.leawind.gitparcel.common.testutils.TestNbt;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
              Mirror.FRONT_BACK,
              Rotation.CLOCKWISE_90,
              // The translation is the anchor's world position: the image of local (7, 3, -2).
              new ParcelTransform(
                      Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, new BlockPos(100, 20, -40))
                  .apply(new BlockPos(7, 3, -2))));

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
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            TEST_ENTITY,
            "Facing",
            ParcelCoordinateField.Encoding.DIRECTION));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            TEST_ENTITY,
            "ItemRotation",
            ParcelCoordinateField.Encoding.ROTATION_STEP));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            TEST_ENTITY,
            "forced_outside",
            ParcelCoordinateField.Encoding.POSITION,
            ParcelCoordinateField.Pointing.OUTSIDE));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            TEST_ENTITY,
            "forced_inside",
            ParcelCoordinateField.Encoding.POSITION,
            ParcelCoordinateField.Pointing.INSIDE));
    registry.register(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            TEST_ENTITY,
            "anchor_axes",
            ParcelCoordinateField.Encoding.BLOCK_POS_AXES));
  }

  @Test
  void restoresDeclaredEntityFieldsToParcelRelativeOnCapture() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(120, 70, -30));
    putBlockPosXyz(data, "home_xyz", new BlockPos(120, 70, -30));
    putPositionCodec(data, "sprint_target", new Vec3(120.5, 70.25, -30.5));
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var captured = processor.captureEntity(captureContext(), record);

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

    var restoredItems = TestNbt.getList(restored.data(), "Items").orElseThrow();
    for (int i = 0; i < 2; i++) {
      assertEquals(
          space.toWorld(new Vec3(10 + i, 64, -5)),
          readPositionCodec(
                  TestNbt.getCompound(
                      TestNbt.getCompound(restoredItems, i).orElseThrow(), "tag")
                      .orElseThrow(),
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

    var restoredPassengers = TestNbt.getList(restored.data(), "Passengers").orElseThrow();
    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readBlockPosCodec(TestNbt.getCompound(restoredPassengers, 0).orElseThrow(), "home")
            .orElseThrow());
    assertEquals(
        new BlockPos(120, 70, -30),
        readBlockPosCodec(TestNbt.getCompound(restoredPassengers, 1).orElseThrow(), "home")
            .orElseThrow());
  }

  @Test
  void roundTripsDeclaredFieldsAcrossEveryTransform() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var placement = new ParcelTransform(mirror, rotation, new BlockPos(-30, 64, 11));
        // Fold the old anchor offset into the translation so it becomes the anchor's position.
        var space =
            new ParcelSpace(
                new ParcelTransform(mirror, rotation, placement.apply(new BlockPos(5, -2, 9))));
        var context =
            new ParcelRecordProcessorContext(space, new ParcelAttachmentSession(), null, null, null, 0L);
        var world = new BlockPos(120, 70, -30);
        var data = entityData(TEST_ENTITY);
        putBlockPosCodec(data, "home", world);
        var record =
            new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

        var captured = processor.captureEntity(context, record);
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

  /** Rule 3.1: direction fields transform as orientations and keep their encoded form. */
  @Test
  void transformsDirectionFieldsPreservingTheirEncodedForm() {
    var stringData = entityData(TEST_ENTITY);
    stringData.put("Facing", StringTag.valueOf("north"));
    var stringRecord = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, stringData, List.of());
    var restoredString = processor.restoreEntity(context(), stringRecord);
    assertEquals(
        space.toWorldDirection(Direction.NORTH).getName(),
        TestNbt.getString(restoredString.data(), "Facing").orElseThrow());

    var byteData = entityData(TEST_ENTITY);
    byteData.put("Facing", ByteTag.valueOf((byte) Direction.NORTH.get3DDataValue()));
    var byteRecord = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, byteData, List.of());
    var restoredByte = processor.restoreEntity(context(), byteRecord);
    assertEquals(
        (byte) space.toWorldDirection(Direction.NORTH).get3DDataValue(),
        TestNbt.getByte(restoredByte.data(), "Facing").orElseThrow());
  }

  /**
   * Rule 3.1: rotation steps are rewritten against the record's own declared facing, read before
   * any rewrite.
   */
  @Test
  void transformsRotationStepsAgainstTheDeclaredFacing() {
    var mirrored =
        new ParcelSpace(
            new ParcelTransform(Mirror.LEFT_RIGHT, Rotation.NONE, new BlockPos(100, 64, 100)));
    var mirroredContext =
        new ParcelRecordProcessorContext(
            mirrored, new ParcelAttachmentSession(), null, fullSemantics(), null, 0L);

    var data = entityData(TEST_ENTITY);
    data.putString("Facing", Direction.SOUTH.getName());
    data.put("ItemRotation", ByteTag.valueOf((byte) 3));
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(mirroredContext, record);

    assertEquals(
        Direction.NORTH.getName(), TestNbt.getString(restored.data(), "Facing").orElseThrow());
    assertEquals((byte) 5, TestNbt.getByte(restored.data(), "ItemRotation").orElseThrow());
  }

  private static ParcelSemantics fullSemantics() {
    return new ParcelSemantics(
        List.of(),
        ParcelCoordinateFieldRegistry.get().fields().stream()
            .map(ParcelSemantics.CoordinateField::of)
            .toList(),
        ParcelEntityRefFieldRegistry.get().fields().stream()
            .map(ParcelSemantics.ReferenceField::of)
            .toList(),
        List.of());
  }

  /**
   * Rule 3.2: on restore, values inside the parcel extent transform to world space; values
   * outside it are outside-pointing world references and stay identical.
   */
  @Test
  void restoresOnlyInsidePointingEdgesWhenGeometryIsKnown() {
    var extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));
    var geometricContext =
        new ParcelRecordProcessorContext(
            space, new ParcelAttachmentSession(), null, fullSemantics(), extent, 0L);

    var data = entityData(TEST_ENTITY);
    putPositionCodec(data, "sprint_target", space.toWorld(new Vec3(1, -1, 2)));
    putPositionCodec(data, "forced_outside", space.toWorld(new Vec3(50, 0, 50)));
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(geometricContext, record);

    assertEquals(
        space.toWorld(new Vec3(1, -1, 2)),
        readPositionCodec(restored.data(), "sprint_target").orElseThrow());
  }

  /** Rule 3.2: outside-pointing edges stay identical on restore, whatever the placement. */
  @Test
  void keepsOutsidePointingEdgesIdenticalOnRestore() {
    var extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));
    var geometricContext =
        new ParcelRecordProcessorContext(
            space, new ParcelAttachmentSession(), null, fullSemantics(), extent, 0L);
    Vec3 outsideWorld = new Vec3(1234.5, 64.25, -987.25);

    var data = entityData(TEST_ENTITY);
    putPositionCodec(data, "sprint_target", outsideWorld);
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(geometricContext, record);

    assertEquals(outsideWorld, readPositionCodec(restored.data(), "sprint_target").orElseThrow());
  }

  /** Rule 3.2: capture stores outside-pointing world values verbatim, without rebasing. */
  @Test
  void capturesOutsidePointingEdgesVerbatim() {
    var extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));
    var session = new ParcelAttachmentSession();
    var captureContext =
        new ParcelRecordProcessorContext(space, session, session, null, extent, 0L);
    // A world point whose parcel-space image falls outside the extent.
    Vec3 worldValue = space.toWorld(new Vec3(50, 0, 50));

    var data = entityData(TEST_ENTITY);
    putPositionCodec(data, "sprint_target", worldValue);
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var captured = processor.captureEntity(captureContext, record);

    assertEquals(worldValue, readPositionCodec(captured.data(), "sprint_target").orElseThrow());
  }

  /** Explicit pointing declarations override the geometric detection. */
  @Test
  void pointingDeclarationsOverrideTheGeometry() {
    var extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));
    var geometricContext =
        new ParcelRecordProcessorContext(
            space, new ParcelAttachmentSession(), null, fullSemantics(), extent, 0L);
    Vec3 outsideWorld = new Vec3(1234.5, 64.25, -987.25);
    Vec3 insideParcel = new Vec3(1, -1, 2);

    var data = entityData(TEST_ENTITY);
    putPositionCodec(data, "forced_outside", insideParcel); // in extent but forced outside
    putPositionCodec(data, "forced_inside", outsideWorld); // outside extent but forced inside
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(geometricContext, record);

    assertEquals(
        insideParcel, readPositionCodec(restored.data(), "forced_outside").orElseThrow());
    assertEquals(
        space.toWorld(outsideWorld),
        readPositionCodec(restored.data(), "forced_inside").orElseThrow());
  }

  @Test
  void leavesMissingPathsUntouched() {
    var data = entityData(TEST_ENTITY);
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    assertTrue(TestNbt.getList(restored.data(), "home").isEmpty());
  }

  /**
   * A capture context: the attachment collector marks the capture direction, so every registered
   * field participates.
   */
  private ParcelRecordProcessorContext captureContext() {
    var session = new ParcelAttachmentSession();
    return new ParcelRecordProcessorContext(space, session, session, null, null, 0L);
  }

  /** A restore context carrying a self-description that records every registered field. */
  private ParcelRecordProcessorContext context() {
    return new ParcelRecordProcessorContext(
        space,
        new ParcelAttachmentSession(),
        null,
        new ParcelSemantics(
            List.of(),
            ParcelCoordinateFieldRegistry.get().fields().stream()
                .map(ParcelSemantics.CoordinateField::of)
                .toList(),
            ParcelEntityRefFieldRegistry.get().fields().stream()
                .map(ParcelSemantics.ReferenceField::of)
                .toList(),
            List.of()),
        null,
        0L);
  }

  /** Rule 7.3: fields the snapshot self-description omits must not be rewritten on restore. */
  @Test
  void restoresOnlyFieldsRecordedByTheSnapshotSelfDescription() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(4, -1, 2));
    putPositionCodec(data, "sprint_target", new Vec3(4.5, -1.25, 2.5));
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var partial =
        new ParcelSemantics(
            List.of(),
            List.of(
                ParcelSemantics.CoordinateField.of(
                    ParcelCoordinateField.forType(
                        ParcelCoordinateField.Target.ENTITY,
                        TEST_ENTITY,
                        "home",
                        ParcelCoordinateField.Encoding.BLOCK_POS))),
            List.of(),
            List.of());
    var partialContext =
        new ParcelRecordProcessorContext(
            space, new ParcelAttachmentSession(), null, partial, null, 0L);

    var restored = processor.restoreEntity(partialContext, record);

    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readBlockPosCodec(restored.data(), "home").orElseThrow());
    assertEquals(
        new Vec3(4.5, -1.25, 2.5),
        readPositionCodec(restored.data(), "sprint_target").orElseThrow(),
        "fields absent from the snapshot self-description must not be rebased");
  }

  /** Rule 7.3: restoring a pre-self-description snapshot rewrites nothing. */
  @Test
  void restoresNothingWithoutASelfDescription() {
    var data = entityData(TEST_ENTITY);
    putBlockPosCodec(data, "home", new BlockPos(4, -1, 2));
    var record =
        new EntityRecord(TEST_ENTITY, new Vec3(0, 0, 0), BlockPos.ZERO, data, List.of());

    var manifestlessContext =
        new ParcelRecordProcessorContext(
            space, new ParcelAttachmentSession(), null, null, null, 0L);

    var restored = processor.restoreEntity(manifestlessContext, record);
    assertEquals(new BlockPos(4, -1, 2), readBlockPosCodec(restored.data(), "home").orElseThrow());
  }

  /** Flat-axes fields rebase through the same pipeline as the single-tag encodings. */
  @Test
  void transformsFlatAxesFieldsOnCaptureAndRestore() {
    var captureData = entityData(TEST_ENTITY);
    putAxes(captureData, "anchor_axes", new BlockPos(120, 70, -30));
    var captureRecord =
        new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, captureData, List.of());

    var captured = processor.captureEntity(captureContext(), captureRecord);
    assertEquals(
        space.toParcel(new BlockPos(120, 70, -30)),
        readAxes(captured.data(), "anchor_axes").orElseThrow());

    var restoreData = entityData(TEST_ENTITY);
    putAxes(restoreData, "anchor_axes", new BlockPos(4, -1, 2));
    var restoreRecord =
        new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, restoreData, List.of());

    var restored = processor.restoreEntity(context(), restoreRecord);
    assertEquals(
        space.toWorld(new BlockPos(4, -1, 2)),
        readAxes(restored.data(), "anchor_axes").orElseThrow());
  }

  /** Partial axes are skipped: all three sibling keys must be present to rebase. */
  @Test
  void skipsFlatAxesFieldsWhenAnyAxisKeyIsMissing() {
    var data = entityData(TEST_ENTITY);
    data.putInt("anchor_axesX", 4);
    data.putInt("anchor_axesY", -1);
    var record = new EntityRecord(TEST_ENTITY, Vec3.ZERO, BlockPos.ZERO, data, List.of());

    var restored = processor.restoreEntity(context(), record);

    assertEquals(4, TestNbt.getInt(restored.data(), "anchor_axesX").orElseThrow());
    assertEquals(-1, TestNbt.getInt(restored.data(), "anchor_axesY").orElseThrow());
    assertTrue(TestNbt.getInt(restored.data(), "anchor_axesZ").isEmpty());
  }

  private static void putAxes(CompoundTag data, String prefix, BlockPos pos) {
    data.putInt(prefix + "X", pos.getX());
    data.putInt(prefix + "Y", pos.getY());
    data.putInt(prefix + "Z", pos.getZ());
  }

  private static java.util.Optional<BlockPos> readAxes(CompoundTag data, String prefix) {
    var x = TestNbt.getInt(data, prefix + "X");
    var y = TestNbt.getInt(data, prefix + "Y");
    var z = TestNbt.getInt(data, prefix + "Z");
    if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(new BlockPos(x.orElseThrow(), y.orElseThrow(), z.orElseThrow()));
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
    data.put(key, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().orElseThrow());
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
    return TestNbt.getCompound(data, key)
        .map(
            compound ->
                new BlockPos(
                    TestNbt.getInt(compound, "X").orElseThrow(),
                    TestNbt.getInt(compound, "Y").orElseThrow(),
                    TestNbt.getInt(compound, "Z").orElseThrow()));
  }

  private static void putPositionCodec(CompoundTag data, String key, Vec3 pos) {
    data.put(key, Vec3.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().orElseThrow());
  }

  private static java.util.Optional<Vec3> readPositionCodec(CompoundTag data, String key) {
    var tag = data.get(key);
    return tag == null ? java.util.Optional.empty() : Vec3.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }
}
