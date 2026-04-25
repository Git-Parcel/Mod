package io.github.leawind.gitparcel.gametest.utils;

import io.github.leawind.gitparcel.mixin.AccessGameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

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

  /**
   * Get absolute bounding box.
   *
   * @see #getBounds
   */
  public BoundingBox getBoundingBox() {
    var aabb = this.getBounds();
    return new BoundingBox(
        (int) aabb.minX,
        (int) aabb.minY,
        (int) aabb.minZ,
        (int) aabb.maxX,
        (int) aabb.maxY,
        (int) aabb.maxZ);
  }

  /**
   * @see #getRelativeBounds
   */
  public BoundingBox getRelativeBoundingBox() {
    var aabb = this.getRelativeBounds();
    return new BoundingBox(
        (int) aabb.minX,
        (int) aabb.minY,
        (int) aabb.minZ,
        (int) aabb.maxX,
        (int) aabb.maxY,
        (int) aabb.maxZ);
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

  /**
   * @see #assertSameBlockEntity
   */
  public void assertSameBlockEntity(BlockPos testPos, BlockPos comparisonPos) {
    var a = this.getBlockEntity(testPos);
    var b = this.getBlockEntity(comparisonPos);
    if (a != null) {
      if (b == null) {
        throw this.assertionException(testPos, "test.error.entity_not_equal", a, "null");
      }

      if (!a.equals(b)) {
        throw this.assertionException(testPos, "test.error.entity_not_equal", a, b);
      }
    }
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
            assertSameBlockEntity(posA, posB);
          }
        }
      }
    }
  }
}
