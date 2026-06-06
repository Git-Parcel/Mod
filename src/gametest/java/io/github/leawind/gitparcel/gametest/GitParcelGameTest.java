package io.github.leawind.gitparcel.gametest;

import com.google.common.jimfs.Jimfs;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.core.api.parcel.config.ConfigItem;
import io.github.leawind.gitparcel.gametest.utils.ChannelFlags;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.mc.storage.ParcelStorage;
import io.github.leawind.gitparcel.testutils.TestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();

  public void testSaveAndLoad(GameTestHelpMore helper) throws Exception {
    TestUtils.forEachFormatCombination(
        ParcelFormatRegistry.INSTANCE.streamSavers().toList(),
        (saver, rotation, mirror) -> {
          if (saver.getDefaultConfig() == null) {
            LOGGER.info(
                "Testing format {} with rotation={} mirror={} config=default",
                saver.spec(),
                rotation,
                mirror);
            doSaveAndLoad(helper, saver, rotation, mirror, null);
          } else {
            testAllConfigCombinations(helper, saver, rotation, mirror);
          }
        });

    helper.succeed();
  }

  /**
   * Test a format with all combinations of its config item values.
   *
   * <p>For enum config items, all enum constants are tested. For booleans, both true and false are
   * tested. Other types are left at their default value to avoid combinatorial explosion.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void testAllConfigCombinations(
      GameTestHelpMore helper, ParcelFormat.Saver<?> rawSaver, Rotation rotation, Mirror mirror)
      throws Exception {

    var saver = (ParcelFormat.Saver) rawSaver;
    ParcelFormatConfig<?> config = saver.getDefaultConfig();

    if (config == null) {
      doSaveAndLoad(helper, saver, rotation, mirror, null);
      return;
    }

    List<Map<String, ?>> combos = TestUtils.generateConfigCombinations(saver);
    for (var combo : combos) {
      config.resetToDefault();

      for (var item : config.listConfigItems()) {
        var val = combo.get(item.name());
        if (val != null) {
          ((ConfigItem<Object>) item).set(val);
        }
      }

      LOGGER.info(
          "Testing format {} with rotation={} mirror={} config={}",
          saver.spec(),
          rotation,
          mirror,
          config.toJson());

      doSaveAndLoad(helper, saver, rotation, mirror, config);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void doSaveAndLoad(
      GameTestHelpMore helper,
      ParcelFormat.Saver<?> rawSaver,
      Rotation rotation,
      Mirror mirror,
      @Nullable ParcelFormatConfig config)
      throws Exception {
    var saver = (ParcelFormat.Saver) rawSaver;

    try (var fs = Jimfs.newFileSystem()) {
      Path tempDir = fs.getPath("/tmp");
      Files.createDirectories(tempDir);

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
          config,
          tempDir,
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
          tempDir,
          false,
          true,
          Block.UPDATE_CLIENTS
              | Block.UPDATE_IMMEDIATE
              | Block.UPDATE_KNOWN_SHAPE
              | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);

      helper.assertSame(bottomBox, topBox, ChannelFlags.BLOCKS);

      LOGGER.info("  Passed: rotation={}, mirror={}", rotation, mirror);
    }
  }
}
