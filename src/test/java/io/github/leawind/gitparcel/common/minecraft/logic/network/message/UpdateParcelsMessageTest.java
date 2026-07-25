package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import java.util.List;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class UpdateParcelsMessageTest extends AbstractGitParcelTest {
  @Test
  void fullSyncReplacesExistingParcels() {
    var existing = parcelAt(0);
    var replacement = parcelAt(10);
    var target = Parcels.singleton(existing);

    UpdateParcelsMessage.fullSync(Parcels.singleton(replacement)).applyTo(target);

    assertEquals(1, target.size());
    assertFalse(target.containsKey(existing.uuid()));
    assertTrue(target.containsKey(replacement.uuid()));
  }

  @Test
  void incrementalUpdateAppliesAdditionsAndRemovals() {
    var removed = parcelAt(0);
    var retained = parcelAt(10);
    var added = parcelAt(20);
    var target = new Parcels(List.of(removed, retained));

    UpdateParcelsMessage.incrementalWithRemovals(List.of(added), List.of(removed.uuid()))
        .applyTo(target);

    assertFalse(target.containsKey(removed.uuid()));
    assertTrue(target.containsKey(retained.uuid()));
    assertTrue(target.containsKey(added.uuid()));
  }

  @Test
  void codecRoundTrips() {
    var parcel = parcelAt(0);
    var expected =
        UpdateParcelsMessage.incrementalWithRemovals(List.of(parcel), List.of(parcelAt(10).uuid()));
    var json = UpdateParcelsMessage.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();

    var actual = UpdateParcelsMessage.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

    assertEquals(expected.fullSync(), actual.fullSync());
    assertEquals(expected.removedUuids(), actual.removedUuids());
    assertEquals(expected.parcels().keySet(), actual.parcels().keySet());
  }

  private static io.github.leawind.gitparcel.common.api.world.Parcel parcelAt(int x) {
    return ParcelFactory.create(
        new BoundingBox(x, 0, 0, x + 1, 1, 1), Mirror.NONE, Rotation.NONE);
  }
}
