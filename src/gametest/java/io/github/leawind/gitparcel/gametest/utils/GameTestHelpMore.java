package io.github.leawind.gitparcel.gametest.utils;

import io.github.leawind.gitparcel.gametest.mixin.AccessGameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.nbt.Tag;
/*? if >=26.1 {*/
import net.minecraft.network.chat.Component;
/*?} else {*/
/*import net.minecraft.gametest.framework.GameTestAssertException;
*//*?}*/
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/*
 * The bounds accessors and the assertion factory are 26.x-only vanilla helpers; pre-26.1 derives
 * the same values through GameTestInfo. Tests never rotate (rotationSteps = 0), so the relative
 * box is simply the structure size at the origin.
 */
public class GameTestHelpMore extends GameTestHelper {

  private AccessGameTestHelper accessor() {
    return (AccessGameTestHelper) this;
  }

  private GameTestHelpMore(GameTestInfo testInfo) {
    super(testInfo);
  }

  public static GameTestHelpMore from(GameTestHelper helper) {
    if (helper instanceof GameTestHelpMore result) {
      return result;
    }

    var result = new GameTestHelpMore(((AccessGameTestHelper) helper).getTestInfo());
    result.accessor().setFinalCheckAdded(((AccessGameTestHelper) helper).getFinalCheckAdded());

    return result;
  }

  /** Get absolute bounding box. */
  public BoundingBox getBoundingBox() {
    /*? if >=26.1 {*/
    var aabb = this.getBounds();
    /*?} else {*/
    /*var aabb = this.accessor().getTestInfo().getStructureBounds();
    // 1.20.1 bounds use the inclusive corner block; expand to the exclusive form 26.x returns.
    aabb = aabb.expandTowards(1.0, 1.0, 1.0);
     *//*?}*/
    return new BoundingBox(
        (int) aabb.minX,
        (int) aabb.minY,
        (int) aabb.minZ,
        (int) Math.ceil(aabb.maxX) - 1,
        (int) Math.ceil(aabb.maxY) - 1,
        (int) Math.ceil(aabb.maxZ) - 1);
  }

  /** Get bounding box relative to the structure origin. */
  public BoundingBox getRelativeBoundingBox() {
    /*? if >=26.1 {*/
    var aabb = this.getRelativeBounds();
    /*?} else {*/
    /*var size = this.accessor().getTestInfo().getStructureSize();
    var aabb = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());
     *//*?}*/
    return new BoundingBox(
        (int) aabb.minX,
        (int) aabb.minY,
        (int) aabb.minZ,
        (int) Math.ceil(aabb.maxX) - 1,
        (int) Math.ceil(aabb.maxY) - 1,
        (int) Math.ceil(aabb.maxZ) - 1);
  }

  /**
   * @see #absolutePos
   */
  public BoundingBox absoluteBoundingBox(BoundingBox box) {
    var from = new BlockPos(box.minX(), box.minY(), box.minZ());
    var to = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
    return BoundingBox.fromCorners(absolutePos(from), absolutePos(to));
  }

  /**
   * @see #getBlockState
   */
  public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
    return this.getLevel().getBlockEntity(this.absolutePos(pos));
  }

  public void assertSameNbtStructure(@Nullable Tag a, @Nullable Tag b, boolean compareListTag) {
    if (!GameTestUtils.compareNbtStructure(a, b, compareListTag)) {
      String msg = String.format("NBT structure is not equal:\n  %s\n  %s", a, b);
      /*? if >=26.1 {*/
      throw assertionException(Component.literal(msg));
      /*?} else {*/
      /*throw new GameTestAssertException(msg);
       *//*?}*/
    }
  }

  public void assertSimilarBlockEntityData(BlockPos testPos, BlockPos comparisonPos) {
    var blockEntityA = this.getBlockEntity(testPos);
    var blockEntityB = this.getBlockEntity(comparisonPos);

    var a = blockEntityA == null ? null : GameTestUtils.saveFullMetadata(getLevel(), blockEntityA);
    var b = blockEntityB == null ? null : GameTestUtils.saveFullMetadata(getLevel(), blockEntityB);

    assertSameNbtStructure(a, b, true);
  }

  public void assertSame(BoundingBox boxA, BoundingBox boxB, @ChannelFlags int flags) {
    int sizeX = boxA.getXSpan();
    int sizeY = boxA.getYSpan();
    int sizeZ = boxA.getZSpan();

    if (boxB.getXSpan() != sizeX) {
      fail("X span is not equal");
    }
    if (boxB.getYSpan() != sizeY) {
      fail("Y span is not equal");
    }
    if (boxB.getZSpan() != sizeZ) {
      fail("Z span is not equal");
    }

    for (int x = 0; x < sizeX; x++) {
      for (int y = 0; y < sizeY; y++) {
        for (int z = 0; z < sizeZ; z++) {
          BlockPos posA = new BlockPos(boxA.minX() + x, boxA.minY() + y, boxA.minZ() + z);
          BlockPos posB = new BlockPos(boxB.minX() + x, boxB.minY() + y, boxB.minZ() + z);

          if ((flags & ChannelFlags.BLOCK_STATE) != 0) {
            assertSameBlockState(posA, posB);
          }

          if ((flags & ChannelFlags.BLOCK_ENTITIY) != 0) {
            assertSimilarBlockEntityData(posA, posB);
          }
        }
      }
    }
  }
}
