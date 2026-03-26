package io.github.leawind.gitparcel.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.testutils.AbstractGitParcelTest;
import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

public class ParcelTest extends AbstractGitParcelTest {

  @Test
  void testGetPivotBlockPosPos() {
    var bounds = BoundingBox.fromCorners(new BlockPos(1, 2, 3), new BlockPos(5, 8, 11));

    assertEquals(
        new BlockPos(1, 2, 3), Parcel.getPivotBlockPos(Mirror.NONE, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(5, 2, 3), Parcel.getPivotBlockPos(Mirror.NONE, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(5, 2, 11),
        Parcel.getPivotBlockPos(Mirror.NONE, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(1, 2, 11),
        Parcel.getPivotBlockPos(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, bounds));

    assertEquals(
        new BlockPos(1, 2, 11), Parcel.getPivotBlockPos(Mirror.LEFT_RIGHT, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(1, 2, 3),
        Parcel.getPivotBlockPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(5, 2, 3),
        Parcel.getPivotBlockPos(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(5, 2, 11),
        Parcel.getPivotBlockPos(Mirror.LEFT_RIGHT, Rotation.COUNTERCLOCKWISE_90, bounds));

    assertEquals(
        new BlockPos(5, 2, 3), Parcel.getPivotBlockPos(Mirror.FRONT_BACK, Rotation.NONE, bounds));
    assertEquals(
        new BlockPos(5, 2, 11),
        Parcel.getPivotBlockPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, bounds));
    assertEquals(
        new BlockPos(1, 2, 11),
        Parcel.getPivotBlockPos(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180, bounds));
    assertEquals(
        new BlockPos(1, 2, 3),
        Parcel.getPivotBlockPos(Mirror.FRONT_BACK, Rotation.COUNTERCLOCKWISE_90, bounds));
  }

  @Test
  void testGetPivot() {
    var boundingBox = new BoundingBox(1, 2, 3, 5, 8, 11);

    assertEquals(new Vec3(1, 2, 3), Parcel.getPivot(Mirror.NONE, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3(6, 2, 3), Parcel.getPivot(Mirror.NONE, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3(6, 2, 12), Parcel.getPivot(Mirror.NONE, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3(1, 2, 12),
        Parcel.getPivot(Mirror.NONE, Rotation.COUNTERCLOCKWISE_90, boundingBox));

    assertEquals(
        new Vec3(1, 2, 12), Parcel.getPivot(Mirror.LEFT_RIGHT, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3(1, 2, 3), Parcel.getPivot(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3(6, 2, 3), Parcel.getPivot(Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3(6, 2, 12),
        Parcel.getPivot(Mirror.LEFT_RIGHT, Rotation.COUNTERCLOCKWISE_90, boundingBox));

    assertEquals(new Vec3(6, 2, 3), Parcel.getPivot(Mirror.FRONT_BACK, Rotation.NONE, boundingBox));
    assertEquals(
        new Vec3(6, 2, 12), Parcel.getPivot(Mirror.FRONT_BACK, Rotation.CLOCKWISE_90, boundingBox));
    assertEquals(
        new Vec3(1, 2, 12),
        Parcel.getPivot(Mirror.FRONT_BACK, Rotation.CLOCKWISE_180, boundingBox));
    assertEquals(
        new Vec3(1, 2, 3),
        Parcel.getPivot(Mirror.FRONT_BACK, Rotation.COUNTERCLOCKWISE_90, boundingBox));
  }

  @Test
  void testParcelWithoutTransform() {
    var boundingBox = new BoundingBox(2, 3, 4, 4, 6, 8);
    var parcel = Parcel.create(boundingBox, Mirror.NONE, Rotation.NONE);

    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeWorldSpace());

    var localPivot = Vec3.ZERO;
    var worldPivot = new Vec3(2, 3, 4);
    assertEquals(worldPivot, parcel.getPivot());

    var localPivotBlockCenter = new Vec3(0.5, 0.5, 0.5);
    var worldPivotBlockCenter = new Vec3(2.5, 3.5, 4.5);
    assertEquals(worldPivotBlockCenter, parcel.getPivotBlockCenter());

    var localPivotBlockPos = BlockPos.ZERO;
    var worldPivotBlockPos = new BlockPos(2, 3, 4);
    assertEquals(worldPivotBlockPos, parcel.getPivotBlockPos());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirror() {
    var boundingBox = new BoundingBox(2, 3, 4, 4, 6, 8);
    var parcel = Parcel.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.NONE);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeWorldSpace());

    var localPivot = Vec3.ZERO;
    var worldPivot = new Vec3(2, 3, 9);
    assertEquals(worldPivot, parcel.getPivot());

    var localPivotBlockCenter = new Vec3(0.5, 0.5, 0.5);
    var worldPivotBlockCenter = new Vec3(2.5, 3.5, 8.5);
    assertEquals(worldPivotBlockCenter, parcel.getPivotBlockCenter());

    var localPivotBlockPos = BlockPos.ZERO;
    var worldPivotBlockPos = new BlockPos(2, 3, 8);
    assertEquals(worldPivotBlockPos, parcel.getPivotBlockPos());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithRotation() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = Parcel.create(boundingBox, Mirror.NONE, Rotation.CLOCKWISE_90);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    var localPivot = Vec3.ZERO;
    var worldPivot = new Vec3(9, 3, 2);
    assertEquals(worldPivot, parcel.getPivot());

    var localPivotBlockCenter = new Vec3(0.5, 0.5, 0.5);
    var worldPivotBlockCenter = new Vec3(8.5, 3.5, 2.5);
    assertEquals(worldPivotBlockCenter, parcel.getPivotBlockCenter());

    var localPivotBlockPos = BlockPos.ZERO;
    var worldPivotBlockPos = new BlockPos(8, 3, 2);
    assertEquals(worldPivotBlockPos, parcel.getPivotBlockPos());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirrorAndRotation1() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = Parcel.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_90);

    assertEquals(new Vec3i(3, 4, 5), parcel.meta().size());
    assertEquals(new Vec3i(3, 4, 5), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    var localPivot = Vec3.ZERO;
    var worldPivot = new Vec3(4, 3, 2);
    assertEquals(worldPivot, parcel.getPivot());

    var localPivotBlockCenter = new Vec3(0.5, 0.5, 0.5);
    var worldPivotBlockCenter = new Vec3(4.5, 3.5, 2.5);
    assertEquals(worldPivotBlockCenter, parcel.getPivotBlockCenter());

    var localPivotBlockPos = BlockPos.ZERO;
    var worldPivotBlockPos = new BlockPos(4, 3, 2);
    assertEquals(worldPivotBlockPos, parcel.getPivotBlockPos());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }

  @Test
  void testParcelWithMirrorAndRotation2() {
    var boundingBox = new BoundingBox(4, 3, 2, 8, 6, 4);
    var parcel = Parcel.create(boundingBox, Mirror.LEFT_RIGHT, Rotation.CLOCKWISE_180);

    assertEquals(new Vec3i(5, 4, 3), parcel.meta().size());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeParcelSpace());
    assertEquals(new Vec3i(5, 4, 3), parcel.getSizeWorldSpace());

    var localPivot = Vec3.ZERO;
    var worldPivot = new Vec3(9, 3, 2);
    assertEquals(worldPivot, parcel.getPivot());

    var localPivotBlockCenter = new Vec3(0.5, 0.5, 0.5);
    var worldPivotBlockCenter = new Vec3(8.5, 3.5, 2.5);
    assertEquals(worldPivotBlockCenter, parcel.getPivotBlockCenter());

    var localPivotBlockPos = BlockPos.ZERO;
    var worldPivotBlockPos = new BlockPos(8, 3, 2);
    assertEquals(worldPivotBlockPos, parcel.getPivotBlockPos());

    assertEquals(boundingBox, parcel.getBoundingBox());
  }
}
