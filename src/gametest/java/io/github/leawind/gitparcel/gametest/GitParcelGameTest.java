package io.github.leawind.gitparcel.gametest;

import com.google.common.jimfs.Jimfs;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.gametest.utils.ChannelFlags;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.gametest.utils.GameTestUtils;
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

  public void testParcelLifecycle(GameTestHelpMore helper) {
    var level = helper.getLevel();
    var service = ParcelService.get(level);
    service.reset();

    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    service.addNewParcel(parcel);
    if (service.getParcel(parcel.uuid()) != parcel) {
      helper.fail("Added parcel is not available through the level service");
    }
    if (ParcelService.get(level).getParcel(parcel.uuid()) != parcel) {
      helper.fail("Level saved data is not shared between service instances");
    }

    var server = level.getServer();
    if (GitParcelWorldSavedData.get(server) != GitParcelWorldSavedData.get(server)) {
      helper.fail("World saved data is not cached by the server");
    }

    parcel.visual().showWireframe(false);
    service.updateParcel(parcel);

    if (service.deleteParcel(parcel.uuid()) != parcel || service.getParcel(parcel.uuid()) != null) {
      helper.fail("Deleted parcel is still registered in the level service");
    }

    helper.succeed();
  }

  public void testSaveAndLoad(GameTestHelpMore helper) throws Exception {
    GameTestUtils.forEachFormatCombination(
        ParcelFormatRegistry.get().streamWriters().toList(),
        (writer, rotation, mirror) -> {
          if (writer.getDefaultConfig() == null) {
            LOGGER.info(
                "Testing format {} with rotation={} mirror={} config=default",
                writer.spec(),
                rotation,
                mirror);
            doSaveAndLoad(helper, writer, rotation, mirror, null);
          } else {
            testAllConfigCombinations(helper, writer, rotation, mirror);
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
      GameTestHelpMore helper, ParcelFormat.Writer<?> rawWriter, Rotation rotation, Mirror mirror)
      throws Exception {

    var writer = (ParcelFormat.Writer) rawWriter;
    ParcelFormatConfig<?> config = writer.getDefaultConfig();

    if (config == null) {
      doSaveAndLoad(helper, writer, rotation, mirror, null);
      return;
    }

    List<Map<String, ?>> combos = GameTestUtils.generateConfigCombinations(writer);
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
          writer.spec(),
          rotation,
          mirror,
          config.toJson());

      doSaveAndLoad(helper, writer, rotation, mirror, config);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void doSaveAndLoad(
      GameTestHelpMore helper,
      ParcelFormat.Writer<?> rawWriter,
      Rotation rotation,
      Mirror mirror,
      @Nullable ParcelFormatConfig config)
      throws Exception {
    var writer = (ParcelFormat.Writer) rawWriter;

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
          writer,
          helper.getLevel(),
          helper.absoluteBoundingBox(bottomBox),
          rotation,
          mirror,
          config,
          tempDir,
          true);

      var reader = ParcelFormatRegistry.get().getReader(writer.spec());
      if (reader == null) {
        LOGGER.info("  Skipped: no reader for format {}", writer.spec());
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
