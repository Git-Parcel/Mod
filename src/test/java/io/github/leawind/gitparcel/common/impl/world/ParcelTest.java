package io.github.leawind.gitparcel.common.impl.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.parcel.ParcelMeta;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftVersion;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

public class ParcelTest extends AbstractGitParcelTest {

  @Test
  void testAnchorPosStatic() {
    var boundingBox = new BoundingBox(1, 2, 3, 5, 8, 11);

    assertEquals(
        new Vec3i(1, 2, 3), Parcel.anchorPos(Mirror.NONE, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3i(6, 2, 3), Parcel.anchorPos(Mirror.NONE, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3i(6, 2, 12), Parcel.anchorPos(Mirror.NONE, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3i(1, 2, 12),
        Parcel.anchorPos(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, boundingBox));

    assertEquals(
        new Vec3i(1, 2, 12), Parcel.anchorPos(Mirror.LEFT_RIGHT, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3i(1, 2, 3),
        Parcel.anchorPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3i(6, 2, 3),
        Parcel.anchorPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3i(6, 2, 12),
        Parcel.anchorPos(Mirror.LEFT_RIGHT, Rotation.COUNTERCLOCKWISE_90, boundingBox));

    assertEquals(
        new Vec3i(6, 2, 3), Parcel.anchorPos(Mirror.FRONT_BACK, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3i(6, 2, 12),
        Parcel.anchorPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3i(1, 2, 12),
        Parcel.anchorPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3i(1, 2, 3),
        Parcel.anchorPos(Mirror.FRONT_BACK, Rotation.COUNTERCLOCKWISE_90, boundingBox));
  }

  @Test
  void testParcelWithoutTransform() {
    var boundingBox = new BoundingBox(2, 3, 4, 4, 6, 8);
    var parcel = ParcelFactory.create(boundingBox, Mirror.NONE, Rotation.NONE);

    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeWorldSpace());
    assertEquals(MinecraftVersion.currentDataVersion(), parcel.meta().dataVersion());

    // New parcels anchor at the local minimum corner, so the anchor equals the
    // orientation-dependent corner the factory derives from the box.
    var worldAnchorPos = new Vec3i(2, 3, 4);
    assertEquals(worldAnchorPos, parcel.anchorPos());
    assertEquals(worldAnchorPos, parcel.transform().translation());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirror() {
    var boundingBox = new BoundingBox(2, 3, 4, 4, 6, 8);
    var parcel = ParcelFactory.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.NONE);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeWorldSpace());

    // New parcels anchor at the local minimum corner, so the anchor equals the
    // orientation-dependent corner the factory derives from the box.
    var worldAnchorPos = new Vec3i(2, 3, 9);
    assertEquals(worldAnchorPos, parcel.anchorPos());
    assertEquals(worldAnchorPos, parcel.transform().translation());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithRotation() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = ParcelFactory.create(boundingBox, Mirror.NONE, Rotation.CLOCKWISE_90);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    // New parcels anchor at the local minimum corner, so the anchor equals the
    // orientation-dependent corner the factory derives from the box.
    var worldAnchorPos = new Vec3i(9, 3, 2);
    assertEquals(worldAnchorPos, parcel.anchorPos());
    assertEquals(worldAnchorPos, parcel.transform().translation());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirrorAndRotation1() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = ParcelFactory.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    // New parcels anchor at the local minimum corner, so the anchor equals the
    // orientation-dependent corner the factory derives from the box.
    var worldAnchorPos = new Vec3i(4, 3, 2);
    assertEquals(worldAnchorPos, parcel.anchorPos());
    assertEquals(worldAnchorPos, parcel.transform().translation());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirrorAndRotation2() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = ParcelFactory.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180);

    assertEquals(new Vec3i(5, 4, 3), parcel.meta().size());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    // New parcels anchor at the local minimum corner, so the anchor equals the
    // orientation-dependent corner the factory derives from the box.
    var worldAnchorPos = new Vec3i(9, 3, 2);
    assertEquals(worldAnchorPos, parcel.anchorPos());
    assertEquals(worldAnchorPos, parcel.transform().translation());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void boundingBoxFollowsAnchorRelativeExtent() {
    // Content spans the anchor-relative extent [-anchor, size - anchor); the world box is the
    // placement image of that extent, not a corner-derived pivot.
    var meta =
        new ParcelMeta(
            Map.of(),
            MinecraftVersion.currentDataVersion(),
            new Vec3i(5, 4, 6),
            new Vec3i(2, 1, 3));
    var parcel =
        Parcel.create(
            meta, new ParcelTransform(Mirror.NONE, Rotation.NONE, new Vec3i(100, 64, 200)));

    assertEquals(new Vec3i(100, 64, 200), parcel.anchorPos());
    assertEquals(
        BoundingBox.fromCorners(new BlockPos(98, 63, 197), new BlockPos(102, 66, 202)),
        parcel.getBoundingBox());
  }

  @Test
  void anchorPosIsIndependentOfBoundsChanges() {
    // The anchor is stored as an absolute position. Adjusting the extent (here: growing the parcel
    // past the anchor on the negative side) keeps the anchor and the anchor-relative frame fixed,
    // so archive coordinates and section grid indices stay valid.
    var anchorWorld = new Vec3i(7, 64, 9);
    var before =
        Parcel.create(
            new ParcelMeta(
                Map.of(), MinecraftVersion.currentDataVersion(),
                new Vec3i(4, 4, 4), Vec3i.ZERO),
            new ParcelTransform(Mirror.NONE, Rotation.NONE, anchorWorld));
    var after =
        Parcel.create(
            new ParcelMeta(
                Map.of(), MinecraftVersion.currentDataVersion(),
                new Vec3i(8, 4, 4), new Vec3i(4, 0, 0)),
            new ParcelTransform(Mirror.NONE, Rotation.NONE, anchorWorld));

    assertEquals(anchorWorld, before.anchorPos());
    assertEquals(anchorWorld, after.anchorPos());
    var spaceBefore = new ParcelSpace(before.transform());
    var spaceAfter = new ParcelSpace(after.transform());
    assertEquals(BlockPos.ZERO, spaceBefore.toParcel(spaceAfter.toWorld(BlockPos.ZERO)));
    assertEquals(
        new BoundingBox(7, 64, 9, 10, 67, 12), before.getBoundingBox());
    assertEquals(
        new BoundingBox(3, 64, 9, 10, 67, 12), after.getBoundingBox());
  }

  @Test
  void serializedParcelDoesNotContainRepositoryLocation() {
    var parcel =
        ParcelFactory.create(new BoundingBox(0, 0, 0, 1, 1, 1), Mirror.NONE, Rotation.NONE);
    parcel.assignDimension("minecraft:overworld");
    var json = (JsonObject) Parcel.CODEC.encodeStart(JsonOps.INSTANCE, parcel).result().orElseThrow();
    assertEquals(false, json.has("location"));
    var decoded = Parcel.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    assertEquals(parcel.uuid(), decoded.uuid());
    assertEquals(parcel.dimension(), decoded.dimension());
  }

  @Test
  void archiveSyncCacheIsAbsentUntilFirstSync() {
    var parcel =
        ParcelFactory.create(new BoundingBox(0, 0, 0, 1, 1, 1), Mirror.NONE, Rotation.NONE);
    assertTrue(parcel.archiveSync().isEmpty());
    var json = (JsonObject) Parcel.CODEC.encodeStart(JsonOps.INSTANCE, parcel).result().orElseThrow();
    assertFalse(json.has("archive_sync"));
    assertTrue(Parcel.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow().archiveSync().isEmpty());
  }

  @Test
  void archiveSyncCacheRoundTripsThroughCodec() {
    var parcel =
        ParcelFactory.create(new BoundingBox(0, 0, 0, 2, 3, 4), Mirror.NONE, Rotation.NONE);
    parcel.setArchiveSync(
        new Parcel.ArchiveSync(new Vec3i(2, 3, 4), new Vec3i(1, 0, 2), 12345L));

    var json = (JsonObject) Parcel.CODEC.encodeStart(JsonOps.INSTANCE, parcel).result().orElseThrow();
    var decoded = Parcel.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

    assertEquals(parcel.archiveSync(), decoded.archiveSync());
    assertEquals(new Vec3i(2, 3, 4), decoded.archiveSync().orElseThrow().size());
    assertEquals(new Vec3i(1, 0, 2), decoded.archiveSync().orElseThrow().anchor());
    assertEquals(12345L, decoded.archiveSync().orElseThrow().repositorySizeBytes());
  }
}
