package io.github.leawind.gitparcel.gametest;

import com.google.common.jimfs.Jimfs;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Format;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelService;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.gametest.utils.ChannelFlags;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.gametest.utils.GameTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();
  private static final int WORLD_UPDATE_FLAGS =
      Block.UPDATE_CLIENTS
          | Block.UPDATE_IMMEDIATE
          | Block.UPDATE_KNOWN_SHAPE
          | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
  private static final GitRepo.CommitIdentity GAMETEST_IDENTITY =
      new GitRepo.CommitIdentity("GameTest", "gametest@gitparcel.local");

  public void testParcelLifecycle(GameTestHelpMore helper) throws Exception {
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

  /** Uses the bottom layer as a fixed parcel workspace and upper layers as version fixtures. */
  public void testLayeredSnapshotBranching(GameTestHelpMore helper) throws Exception {
    var structureBox = helper.getRelativeBoundingBox();
    requireDimensions(helper, structureBox, 6, 30, 10, "layered snapshot fixture");

    var workspace =
        new BoundingBox(
            structureBox.minX(),
            structureBox.minY(),
            structureBox.minZ(),
            structureBox.maxX(),
            structureBox.minY() + 4,
            structureBox.maxZ());
    var rootBlocks = captureBlocks(helper, workspace);

    var service = ParcelService.get(helper.getLevel());
    service.reset();
    var parcel =
        ParcelFactory.create(helper.absoluteBoundingBox(workspace), Mirror.NONE, Rotation.NONE);
    service.addNewParcel(parcel);

    var root = service.saveSnapshot(parcel, "Bottom layer", "", GAMETEST_IDENTITY, true);

    copyLayerToWorkspace(helper, structureBox.minY() + 10, workspace);
    var middleBlocks = captureBlocks(helper, workspace);
    if (rootBlocks.equals(middleBlocks)) {
      helper.fail("Bottom and middle layer fixtures must contain different block states");
    }
    var originalChild =
        service.saveSnapshot(parcel, "Middle layer", "", GAMETEST_IDENTITY, true);

    service.restoreSnapshot(
        parcel,
        root,
        RestoreSnapshotRequest.Mode.DIRECT,
        true,
        GAMETEST_IDENTITY);
    assertBlocks(helper, rootBlocks);

    copyLayerToWorkspace(helper, structureBox.minY() + 25, workspace);
    var topBlocks = captureBlocks(helper, workspace);
    if (rootBlocks.equals(topBlocks) || middleBlocks.equals(topBlocks)) {
      helper.fail("Top layer fixture must differ from the bottom and middle layer fixtures");
    }
    var fork = service.saveSnapshot(parcel, "Top layer fork", "", GAMETEST_IDENTITY, true);
    var page = service.querySnapshotTree(parcel, 10, Optional.empty());
    var parents =
        page.nodes().stream()
            .collect(java.util.stream.Collectors.toMap(SnapshotNode::id, SnapshotNode::parentId));
    if (page.nodes().size() != 3
        || !page.current().orElseThrow().equals(fork)
        || !parents.get(originalChild).orElseThrow().equals(root)
        || !parents.get(fork).orElseThrow().equals(root)) {
      helper.fail("Restoring an old snapshot and saving did not produce the expected fork");
    }

    service.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /** Round-trips the complete 30-block-tall fixture with D16, crossing a section boundary. */
  public void testLayeredD16RoundTrip(GameTestHelpMore helper) throws Exception {
    var box = helper.getRelativeBoundingBox();
    requireDimensions(helper, box, 6, 30, 10, "D16 boundary fixture");
    var expected = captureBlocks(helper, box);
    var rawWriter = ParcelFormatRegistry.get().getWriter(ParcellaD16Format.SPEC);
    if (rawWriter == null) {
      helper.fail("D16 writer is not registered");
      return;
    }

    try (var fs = Jimfs.newFileSystem()) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(
          rawWriter,
          helper.getLevel(),
          helper.absoluteBoundingBox(box),
          Rotation.NONE,
          Mirror.NONE,
          null,
          tempDir,
          true);

      fill(helper, box, Blocks.AIR.defaultBlockState());
      ParcelStorage.load(
          helper.getLevel(),
          helper.absoluteBoundingBox(box),
          Rotation.NONE,
          Mirror.NONE,
          tempDir,
          false,
          true,
          WORLD_UPDATE_FLAGS);
    }

    assertBlocks(helper, expected);
    helper.succeed();
  }

  /** Performs exactly one representative snapshot save and restore through the production service. */
  public void testNormalSnapshotRoundTrip(GameTestHelpMore helper) throws Exception {
    var box = helper.getRelativeBoundingBox();
    requireDimensions(helper, box, 48, 48, 48, "representative snapshot fixture");

    var service = ParcelService.get(helper.getLevel());
    service.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    if (!parcel.meta().formatSpec().equals(ParcellaD32Format.SPEC)) {
      helper.fail("Representative snapshot test must use the default D32 format");
    }
    service.addNewParcel(parcel);

    var sentinelPositions = normalStructureSentinels();
    var expected = captureBlocks(helper, sentinelPositions);
    var root =
        service.saveSnapshot(parcel, "Representative terrain", "", GAMETEST_IDENTITY, true);

    for (BlockPos pos : sentinelPositions) {
      BlockState original = helper.getBlockState(pos);
      BlockState replacement =
          original.is(Blocks.BARRIER)
              ? Blocks.GOLD_BLOCK.defaultBlockState()
              : Blocks.BARRIER.defaultBlockState();
      helper.getLevel().setBlock(helper.absolutePos(pos), replacement, WORLD_UPDATE_FLAGS);
    }

    var result =
        service.restoreSnapshot(
            parcel,
            root,
            RestoreSnapshotRequest.Mode.DIRECT,
            true,
            GAMETEST_IDENTITY);
    if (!result.restored().equals(root)) {
      helper.fail("Representative snapshot restore reported an unexpected snapshot");
    }
    assertBlocks(helper, expected);

    var page = service.querySnapshotTree(parcel, 10, Optional.empty());
    if (page.nodes().size() != 1 || !page.current().orElseThrow().equals(root)) {
      helper.fail("Representative snapshot repository does not contain exactly one snapshot");
    }
    if (!service.pendingRestoreOperations(parcel).isEmpty()) {
      helper.fail("Representative snapshot restore left a pending recovery operation");
    }

    service.deleteParcel(parcel.uuid());
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

  private static void requireDimensions(
      GameTestHelpMore helper,
      BoundingBox box,
      int sizeX,
      int sizeY,
      int sizeZ,
      String fixtureName) {
    if (box.getXSpan() != sizeX || box.getYSpan() != sizeY || box.getZSpan() != sizeZ) {
      helper.fail(
          "%s has unexpected dimensions: expected %dx%dx%d, got %dx%dx%d"
              .formatted(
                  fixtureName,
                  sizeX,
                  sizeY,
                  sizeZ,
                  box.getXSpan(),
                  box.getYSpan(),
                  box.getZSpan()));
    }
  }

  private static BlockSnapshot captureBlocks(GameTestHelpMore helper, BoundingBox box) {
    var positions = new ArrayList<BlockPos>(box.getXSpan() * box.getYSpan() * box.getZSpan());
    for (int y = box.minY(); y <= box.maxY(); y++) {
      for (int x = box.minX(); x <= box.maxX(); x++) {
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
          positions.add(new BlockPos(x, y, z));
        }
      }
    }
    return captureBlocks(helper, positions);
  }

  private static BlockSnapshot captureBlocks(
      GameTestHelpMore helper, List<BlockPos> positions) {
    var states = new ArrayList<BlockState>(positions.size());
    for (BlockPos pos : positions) {
      states.add(helper.getBlockState(pos));
    }
    return new BlockSnapshot(positions, states);
  }

  private static void assertBlocks(GameTestHelpMore helper, BlockSnapshot expected) {
    for (int i = 0; i < expected.positions().size(); i++) {
      BlockPos pos = expected.positions().get(i);
      BlockState actual = helper.getBlockState(pos);
      BlockState expectedState = expected.states().get(i);
      if (!actual.equals(expectedState)) {
        helper.fail(
            "Block mismatch at %s: expected %s, got %s"
                .formatted(pos.toShortString(), expectedState, actual));
      }
    }
  }

  private static void copyLayerToWorkspace(
      GameTestHelpMore helper, int sourceMinY, BoundingBox workspace) {
    for (int y = 0; y < workspace.getYSpan(); y++) {
      for (int x = workspace.minX(); x <= workspace.maxX(); x++) {
        for (int z = workspace.minZ(); z <= workspace.maxZ(); z++) {
          BlockState state = helper.getBlockState(new BlockPos(x, sourceMinY + y, z));
          var target = new BlockPos(x, workspace.minY() + y, z);
          helper.getLevel().setBlock(helper.absolutePos(target), state, WORLD_UPDATE_FLAGS);
        }
      }
    }
  }

  private static void fill(GameTestHelpMore helper, BoundingBox box, BlockState state) {
    for (int y = box.minY(); y <= box.maxY(); y++) {
      for (int x = box.minX(); x <= box.maxX(); x++) {
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
          helper
              .getLevel()
              .setBlock(helper.absolutePos(new BlockPos(x, y, z)), state, WORLD_UPDATE_FLAGS);
        }
      }
    }
  }

  private static List<BlockPos> normalStructureSentinels() {
    return List.of(
        new BlockPos(0, 0, 0),
        new BlockPos(47, 0, 47),
        new BlockPos(0, 47, 47),
        new BlockPos(47, 47, 0),
        new BlockPos(15, 15, 15),
        new BlockPos(16, 16, 16),
        new BlockPos(31, 31, 31),
        new BlockPos(32, 32, 32),
        new BlockPos(31, 24, 24),
        new BlockPos(32, 24, 24),
        new BlockPos(24, 31, 24),
        new BlockPos(24, 32, 24),
        new BlockPos(24, 24, 31),
        new BlockPos(24, 24, 32),
        new BlockPos(24, 24, 24),
        new BlockPos(46, 46, 46));
  }

  private record BlockSnapshot(List<BlockPos> positions, List<BlockState> states) {
    private BlockSnapshot {
      positions = List.copyOf(positions);
      states = List.copyOf(states);
    }
  }
}
