package io.github.leawind.gitparcel.gametest;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.ParcelStorage;
import io.github.leawind.gitparcel.gametest.utils.ChannelFlags;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.gametest.utils.GitParcelTestUtils;
import io.github.leawind.inventory.misc.TempDirectory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();

  public void testSaveAndLoad(GameTestHelpMore helper) throws Exception {
    GitParcelTestUtils.forEachFormatCombination(
        ParcelFormatRegistry.INSTANCE.streamSavers().toList(),
        (saver, rotation, mirror) -> {
          LOGGER.info(
              "Testing format {} with rotation={} mirror={}", saver.spec(), rotation, mirror);

          try (var tempDir = new TempDirectory(GitParcel.MOD_ID)) {
            var box = helper.getRelativeBoundingBox();
            int halfHeight = box.getYSpan() / 2;

            var bottomBox =
                new BoundingBox(
                    box.minX(),
                    box.minY(),
                    box.minZ(),
                    box.maxX(),
                    box.minY() + halfHeight - 1,
                    box.maxZ());

            var topBox =
                new BoundingBox(
                    box.minX(),
                    box.maxY() + 1 - halfHeight,
                    box.minZ(),
                    box.maxX(),
                    box.maxY(),
                    box.maxZ());

            ParcelStorage.save(
                saver,
                helper.getLevel(),
                helper.absoluteBoundingBox(bottomBox),
                rotation,
                mirror,
                null,
                tempDir.getPath(),
                true);

            var loader = ParcelFormatRegistry.INSTANCE.getLoader(saver.spec());
            if (loader == null) {
              LOGGER.info("  Skipped: no loader for format {}", saver.spec());
              return;
            }

            ParcelStorage.load(
                helper.getLevel(),
                helper.absoluteBoundingBox(topBox),
                rotation,
                mirror,
                tempDir.getPath(),
                false,
                true,
                Block.UPDATE_CLIENTS
                    | Block.UPDATE_IMMEDIATE
                    | Block.UPDATE_KNOWN_SHAPE
                    | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

            helper.assertSame(bottomBox, topBox, ChannelFlags.BLOCKS);

            LOGGER.info("  Passed: rotation={}, mirror={}", rotation, mirror);
          }
        });

    helper.succeed();
  }
}
