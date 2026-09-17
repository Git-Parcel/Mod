package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientFieldRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.List;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransientFieldProcessorTest extends AbstractMinecraftTest {
  private static final Identifier TEST_ENTITY = id("entity");
  private static final Identifier OTHER_ENTITY = id("other");
  private static final Identifier TEST_BLOCK_ENTITY = id("block_entity");
  private static final ParcelTransientField ELIMINATE_FIRE =
      ParcelTransientField.forAny(
          ParcelTransientField.Target.ENTITY, "Fire", ParcelTransientField.Kind.ELIMINATE);
  private static final ParcelTransientField OFFSET_ANGER =
      ParcelTransientField.forType(
          ParcelTransientField.Target.ENTITY,
          TEST_ENTITY,
          "anger_end_time",
          ParcelTransientField.Kind.OFFSET_GAME_TIME);
  private static final ParcelTransientField ELIMINATE_PASSENGER_HURT =
      ParcelTransientField.forAny(
          ParcelTransientField.Target.ENTITY, "HurtTime", ParcelTransientField.Kind.ELIMINATE);
  private static final ParcelTransientField OFFSET_NESTED_RESUMES_AT =
      ParcelTransientField.forType(
          ParcelTransientField.Target.BLOCK_ENTITY,
          TEST_BLOCK_ENTITY,
          "server_data.state_updating_resumes_at",
          ParcelTransientField.Kind.OFFSET_GAME_TIME);

  private final TransientFieldProcessor processor = new TransientFieldProcessor();

  @BeforeAll
  static void registerDeclarations() {
    var registry = ParcelTransientFieldRegistry.get();
    registry.register(ELIMINATE_FIRE);
    registry.register(OFFSET_ANGER);
    registry.register(ELIMINATE_PASSENGER_HURT);
    registry.register(OFFSET_NESTED_RESUMES_AT);
  }

  @Test
  void eliminatesDeclaredFieldsOnCaptureOnly() {
    var record = recordWithFire(10);

    var captured = processor.captureEntity(context(null), null, record);
    assertFalse(captured.data().contains("Fire"), "eliminated fields must not enter snapshots");

    // The restore side performs no elimination: snapshots never carry the fields, and a
    // hand-made or legacy record keeps its values.
    var restored = processor.restoreEntity(context(null), recordWithFire(10));
    assertTrue(restored.data().contains("Fire"));
    var fire = restored.data().getShort("Fire").orElseThrow();
    assertEquals(10, (int) fire);
  }

  @Test
  void keepsUndeclaredFieldsUntouched() {
    var record = recordWithFire(10);
    record.data().put("Health", ByteTag.valueOf((byte) 20));

    var captured = processor.captureEntity(context(null), null, record);

    assertTrue(captured.data().contains("Health"));
    assertTrue(captured.data().contains("Fire") == false);
  }

  /** Rule 7.3: restores re-anchor offsets only when the snapshot self-description declares them. */
  @Test
  void offsetsParticipateOnlyWhenTheManifestDeclaresThem() {
    var data = new CompoundTag();
    data.putString("id", TEST_ENTITY.toString());
    data.put("anger_end_time", LongTag.valueOf(5000L));

    var manifestless = context(null);
    var restored = processor.restoreEntity(manifestless, record(TEST_ENTITY, data.copy()));
    assertEquals(
        5000L, restored.data().getLong("anger_end_time").orElseThrow(), "no manifest, no rewrite");

    var declaring =
        new ParcelSemantics(
            List.of(),
            List.of(),
            List.of(),
            List.of(ParcelSemantics.TransientField.of(OFFSET_ANGER)),
            List.of());
    var withManifest =
        new ParcelRecordProcessorContext(
            null, SPACE, new ParcelAttachmentSession(), null, declaring, null);
    var reAnchored = processor.restoreEntity(withManifest, record(TEST_ENTITY, data.copy()));
    // Test levels are absent, so the current game time reads as zero: value + 0.
    assertEquals(5000L, reAnchored.data().getLong("anger_end_time").orElseThrow());
  }

  /** Elimination descends into the Passengers subtree filtered by each nested type. */
  @Test
  void eliminatesInsidePassengerSubtrees() {
    var data = entityData(TEST_ENTITY);
    data.put("HurtTime", ByteTag.valueOf((byte) 3));
    var passenger = entityData(OTHER_ENTITY);
    passenger.put("HurtTime", ByteTag.valueOf((byte) 4));
    data.put("Passengers", passengerList(passenger));
    var record = record(TEST_ENTITY, data);

    var captured = processor.captureEntity(context(null), null, record);

    assertFalse(captured.data().contains("HurtTime"));
    assertFalse(
        captured
            .data()
            .getList("Passengers")
            .orElseThrow()
            .getCompound(0)
            .orElseThrow()
            .contains("HurtTime"));
  }

  /** Nested paths re-anchor values inside nested compounds, leaving sibling data untouched. */
  @Test
  void offsetsNestedBlockEntityPaths() {
    var data = new CompoundTag();
    data.putString("id", TEST_BLOCK_ENTITY.toString());
    var serverData = new CompoundTag();
    serverData.putLong("state_updating_resumes_at", 5000L);
    serverData.putString("unchanged", "value");
    data.put("server_data", serverData);
    var record = new BlockEntityRecord(BlockPos.ZERO, data, List.of());

    var captured = processor.captureBlockEntity(context(null), null, record);
    // Test levels are absent so the game time reads as zero: offset = value - 0.
    var capturedServerData =
        captured.data().getCompound("server_data").orElseThrow();
    assertEquals(5000L, capturedServerData.getLong("state_updating_resumes_at").orElseThrow());
    assertEquals("value", capturedServerData.getString("unchanged").orElseThrow());

    var declaring =
        new ParcelSemantics(
            List.of(),
            List.of(),
            List.of(),
            List.of(ParcelSemantics.TransientField.of(OFFSET_NESTED_RESUMES_AT)),
            List.of());
    var restored = processor.restoreBlockEntity(context(declaring), captured);
    assertEquals(
        5000L,
        restored
            .data()
            .getCompound("server_data")
            .orElseThrow()
            .getLong("state_updating_resumes_at")
            .orElseThrow());
  }

  private ParcelRecordProcessorContext context(ParcelSemantics semantics) {
    return new ParcelRecordProcessorContext(
        null, SPACE, new ParcelAttachmentSession(), null, semantics, null);
  }

  private static EntityRecord recordWithFire(int seconds) {
    var data = entityData(TEST_ENTITY);
    data.put("Fire", ShortTag.valueOf((short) seconds));
    return record(TEST_ENTITY, data);
  }

  private static EntityRecord record(Identifier type, CompoundTag data) {
    return new EntityRecord(type, Vec3.ZERO, BlockPos.ZERO, data, List.of());
  }

  private static CompoundTag entityData(Identifier type) {
    var data = new CompoundTag();
    data.putString("id", type.toString());
    return data;
  }

  private static net.minecraft.nbt.ListTag passengerList(CompoundTag passenger) {
    var list = new ListTag();
    list.add(passenger);
    return list;
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath("gitparceltest", path);
  }

  private static final ParcelSpace SPACE =
      new ParcelSpace(new ParcelTransform(Mirror.NONE, Rotation.NONE, new BlockPos(0, 64, 0)));
}
