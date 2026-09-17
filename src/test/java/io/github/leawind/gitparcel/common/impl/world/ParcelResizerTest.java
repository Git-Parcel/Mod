package io.github.leawind.gitparcel.common.impl.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.testutils.AbstractGitParcelTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

class ParcelResizerTest extends AbstractGitParcelTest {

  @Test
  void keepsTheAnchorFixedWhileRewritingTheExtent() {
    for (Mirror mirror : Mirror.values()) {
      for (Rotation rotation : Rotation.values()) {
        var originalBox = new BoundingBox(100, 64, 200, 105, 69, 207);
        var parcel = ParcelFactory.create(originalBox, mirror, rotation);
        var anchorBefore = parcel.anchorPos();

        var newBox = new BoundingBox(95, 60, 210, 108, 75, 215);
        ParcelResizer.resize(parcel, newBox);

        assertEquals(anchorBefore, parcel.anchorPos(), "mirror=%s rotation=%s".formatted(mirror, rotation));
        assertEquals(
            newBox,
            parcel.getBoundingBox(),
            "world box must equal the requested extent for mirror=%s rotation=%s"
                .formatted(mirror, rotation));
        assertEquals(
            new Vec3i(newBox.getXSpan(), newBox.getYSpan(), newBox.getZSpan()),
            parcel.getSizeWorldSpace());
      }
    }
  }

  /** Extending past the anchor turns it into an interior point of the extent. */
  @Test
  void anchorMayEndUpInsideTheExtent() {
    var parcel = ParcelFactory.create(new BoundingBox(10, 64, 10, 13, 67, 13), Mirror.NONE, Rotation.NONE);
    assertEquals(new Vec3i(10, 64, 10), parcel.anchorPos());

    ParcelResizer.resize(parcel, new BoundingBox(6, 64, 8, 13, 67, 13));

    assertEquals(new Vec3i(10, 64, 10), parcel.anchorPos());
    assertEquals(new Vec3i(4, 0, 2), parcel.meta().anchor());
    assertEquals(new BoundingBox(6, 64, 8, 13, 67, 13), parcel.getBoundingBox());
  }

  /** The archive stays address-stable: content is keyed relative to the unmoved anchor. */
  @Test
  void anchorRelativeFrameIsUnchangedByResize() {
    var parcel = ParcelFactory.create(new BoundingBox(50, 64, 50, 55, 69, 55), Mirror.NONE, Rotation.NONE);
    var spaceBefore = new io.github.leawind.gitparcel.common.api.parcel.ParcelSpace(parcel.transform());

    ParcelResizer.resize(parcel, new BoundingBox(48, 64, 48, 60, 75, 60));

    var spaceAfter = new io.github.leawind.gitparcel.common.api.parcel.ParcelSpace(parcel.transform());
    assertEquals(spaceBefore, spaceAfter);
  }

  /** Corner order is irrelevant: the resized extent normalizes, like creation via fromCorners. */
  @Test
  void cornerOrderIsIrrelevant() {
    var forward = ParcelFactory.create(new BoundingBox(0, 64, 0, 3, 67, 3), Mirror.NONE, Rotation.NONE);
    var reversed = ParcelFactory.create(new BoundingBox(0, 64, 0, 3, 67, 3), Mirror.NONE, Rotation.NONE);

    ParcelResizer.resize(forward, new BoundingBox(10, 70, 10, 5, 66, 15));
    ParcelResizer.resize(reversed, new BoundingBox(5, 66, 15, 10, 70, 10));

    assertEquals(forward.meta().size(), reversed.meta().size());
    assertEquals(forward.meta().anchor(), reversed.meta().anchor());
    assertEquals(forward.getBoundingBox(), reversed.getBoundingBox());
  }
}
