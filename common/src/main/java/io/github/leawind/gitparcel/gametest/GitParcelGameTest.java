package io.github.leawind.gitparcel.gametest;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.inventory.misc.TempDirectory;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();

  public void testBlockPlacement(GameTestHelper helper) {

    BlockPos pos = new BlockPos(1, 1, 1);

    helper.setBlock(pos, Blocks.STONE);
    helper.assertBlockPresent(Blocks.STONE, pos);

    helper.succeed();
  }

  public void testAirBlock(GameTestHelper helper) {
    BlockPos pos = new BlockPos(0, 0, 0);
    helper.assertBlockPresent(Blocks.AIR, pos);
    helper.succeed();
  }

  public void testMultipleBlocks(GameTestHelper helper) {
    helper.setBlock(new BlockPos(1, 0, 0), Blocks.COBBLESTONE);
    helper.setBlock(new BlockPos(0, 0, 1), Blocks.DIRT);
    helper.setBlock(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS);

    helper.assertBlockPresent(Blocks.COBBLESTONE, 1, 0, 0);
    helper.assertBlockPresent(Blocks.DIRT, 0, 0, 1);
    helper.assertBlockPresent(Blocks.OAK_PLANKS, 1, 0, 1);

    helper.succeed();
  }

  public void testSave(GameTestHelpMore helper) throws IOException, ParcelException {
    var saver = ParcelFormatRegistry.INSTANCE.defaultSaver();
    Rotation rotation = Rotation.NONE;
    Mirror mirror = Mirror.NONE;

    try (var tempDir = new TempDirectory(GitParcel.MOD_ID)) {
      LOGGER.info("Temp directory: {}", tempDir.getPath());

      saver.save(
          helper.getLevel(),
          helper.getBoundingBox(),
          rotation,
          mirror,
          null,
          tempDir.getPath(),
          true);
      LOGGER.info("Saved");
    }

    helper.succeed();
  }
}
