package io.github.leawind.gitparcel.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.testutils.AbstractGitParcelTest;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

public class ParcelsTest extends AbstractGitParcelTest {
  @Test
  void test() {
    Parcels parcels = new Parcels();

    var parcel =
        Parcel.create(
            BoundingBox.fromCorners(new Vec3i(0, 0, 0), new Vec3i(10, 10, 10)),
            Mirror.NONE,
            Rotation.NONE);

    parcels.put(parcel);

    var parcel2 = parcels.get(parcel.uuid());

    assertEquals(parcel, parcel2);

    // codec
    var parcelsJson = Parcels.CODEC.encodeStart(JsonOps.INSTANCE, parcels).getOrThrow();
    var parcels2 = Parcels.CODEC.parse(JsonOps.INSTANCE, parcelsJson).getOrThrow();
    assertEquals(parcels.keySet(), parcels2.keySet());
  }

  @Test
  void test2() {}
}
