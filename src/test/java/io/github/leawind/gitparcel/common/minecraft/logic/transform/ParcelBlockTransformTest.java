package io.github.leawind.gitparcel.common.minecraft.logic.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

class ParcelBlockTransformTest extends AbstractMinecraftTest {

  @Test
  void convertsBlockStateOrientationRoundtrip() {
    var localState = Blocks.OAK_STAIRS.defaultBlockState();

    for (var mirror : Mirror.values()) {
      for (var rotation : Rotation.values()) {
        var transform = new ParcelTransform(mirror, rotation, Vec3i.ZERO);
        var worldState = ParcelBlockTransform.toWorldSpace(transform, localState);

        assertEquals(localState.mirror(mirror).rotate(rotation), worldState);
        assertEquals(localState, ParcelBlockTransform.toParcelSpace(transform, worldState));
      }
    }
  }
}
