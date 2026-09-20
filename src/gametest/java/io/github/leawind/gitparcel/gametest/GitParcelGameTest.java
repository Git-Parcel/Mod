package io.github.leawind.gitparcel.gametest;

import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotNode;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.ParcelFactory;
import io.github.leawind.gitparcel.common.utils.git.GitRepositoryCore;
import io.github.leawind.gitparcel.common.utils.git.SharedRepository;
import io.github.leawind.gitparcel.gametest.ext.MarkerRecordProcessor;
import io.github.leawind.gitparcel.gametest.ext.RegionMarkerContributor;
import io.github.leawind.gitparcel.gametest.utils.ChannelFlags;
import io.github.leawind.gitparcel.gametest.utils.GameEntityTypes;
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import io.github.leawind.gitparcel.gametest.utils.GameTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
/*? if >=26.1 {*/
import net.minecraft.core.component.DataComponents;
/*?}*/
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
/*? if >=26.1 {*/
import net.minecraft.util.ProblemReporter;
/*?}*/
import net.minecraft.world.entity.EntityType;
/*? if >=26.1 {*/
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
/*?} else {*/
/*import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
*//*?}*/
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
/*? if >=26.1 {*/
import net.minecraft.world.level.saveddata.maps.MapId;
/*? if >=26.1 {*/
import net.minecraft.world.level.storage.TagValueOutput;
/*?}*/
import net.minecraft.world.level.storage.TagValueInput;
/*?}*/
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.NbtReads;
import java.util.UUID;
import net.minecraft.nbt.Tag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
/*? if >=26.1 {*/
import net.minecraft.world.entity.decoration.painting.Painting;
/*?} else {*/
/*import net.minecraft.world.entity.decoration.Painting;
*//*?}*/
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
/*? if >=26.1 {*/
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.item.component.LodestoneTracker;
/*?} else {*/
/*import net.minecraft.world.entity.decoration.PaintingVariants;
*//*?}*/
import org.slf4j.Logger;

public class GitParcelGameTest {
  public static final Logger LOGGER = LogUtils.getLogger();
  private static final int WORLD_UPDATE_FLAGS =
      Block.UPDATE_CLIENTS
          | Block.UPDATE_IMMEDIATE
          | Block.UPDATE_KNOWN_SHAPE
          | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
  private static final GitRepositoryCore.Identity GAMETEST_IDENTITY =
      new GitRepositoryCore.Identity("GameTest", "gametest@gitparcel.local");

  public void testParcelLifecycle(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();

    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);
    if (registry.getParcel(parcel.uuid()) != parcel) {
      helper.fail("Added parcel is not available through the level service");
    }
    if (ParcelRegistry.get(level).getParcel(parcel.uuid()) != parcel) {
      helper.fail("Level saved data is not shared between service instances");
    }

    var server = level.getServer();
    if (GitParcelWorldSavedData.get(server) != GitParcelWorldSavedData.get(server)) {
      helper.fail("World saved data is not cached by the server");
    }

    parcel.visual().showWireframe(false);
    registry.updateParcel(parcel);

    if (registry.deleteParcel(parcel.uuid()) != parcel || registry.getParcel(parcel.uuid()) != null) {
      helper.fail("Deleted parcel is still registered in the level service");
    }

    helper.succeed();
  }

  public void testSaveAndLoad(GameTestHelpMore helper) throws Exception {
    for (var sectionSize : BlockContentType.BlockSectionSize.values()) {
      for (Rotation rotation : Rotation.values()) {
        for (Mirror mirror : Mirror.values()) {
          LOGGER.info(
              "Testing content round trip with sectionSize={} rotation={} mirror={}",
              sectionSize.edgeLength(),
              rotation,
              mirror);
          doSaveAndLoad(helper, rotation, mirror, sectionSize);
        }
      }
    }

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

    var registry = ParcelRegistry.get(helper.getLevel());
    var service = SnapshotService.get(helper.getLevel());
    registry.reset();
    var parcel =
        ParcelFactory.create(helper.absoluteBoundingBox(workspace), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    var root = service.saveSnapshot(parcel, "Bottom layer", "", GAMETEST_IDENTITY, true);
    assertArchiveSyncRefreshed(helper, parcel, "save");

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
    assertArchiveSyncRefreshed(helper, parcel, "restore");
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

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /** Round-trips the complete 30-block-tall fixture with 16-block sections. */
  public void testLayeredD16RoundTrip(GameTestHelpMore helper) throws Exception {
    var box = helper.getRelativeBoundingBox();
    requireDimensions(helper, box, 6, 30, 10, "16-block section boundary fixture");
    var expected = captureBlocks(helper, box);
    var parcel =
        ParcelFactory.create(helper.absoluteBoundingBox(box), Mirror.NONE, Rotation.NONE);
    configureBlockSectionSize(parcel, BlockContentType.BlockSectionSize.SIZE_16);

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(helper.getLevel(), parcel, tempDir, true);

      fill(helper, box, Blocks.AIR.defaultBlockState());
      ParcelStorage.load(
          helper.getLevel(),
          parcel.transform(),
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

    var registry = ParcelRegistry.get(helper.getLevel());
    var service = SnapshotService.get(helper.getLevel());
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    var blocksConfig = blockConfig(parcel);
    if (blocksConfig.sectionSize.get() != BlockContentType.BlockSectionSize.SIZE_32) {
      helper.fail("Representative snapshot test must use 32-block sections by default");
    }
    registry.addNewParcel(parcel);

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

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /** A SAVE_THEN_RESTORE must persist a protective snapshot before rewinding the world. */
  public void testSaveThenRestoreKeepsProtectiveSnapshot(GameTestHelpMore helper)
      throws Exception {
    var registry = ParcelRegistry.get(helper.getLevel());
    var service = SnapshotService.get(helper.getLevel());
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    var probe = BlockPos.ZERO;
    BlockState original = helper.getBlockState(probe);
    BlockState replacement =
        original.is(Blocks.GOLD_BLOCK)
            ? Blocks.DIAMOND_BLOCK.defaultBlockState()
            : Blocks.GOLD_BLOCK.defaultBlockState();

    var root = service.saveSnapshot(parcel, "Base", "", GAMETEST_IDENTITY, true);
    helper.getLevel().setBlock(helper.absolutePos(probe), replacement, WORLD_UPDATE_FLAGS);
    if (helper.getBlockState(probe) == original) {
      helper.fail("Test mutation did not change the probe block");
    }

    var result =
        service.restoreSnapshot(
            parcel, root, RestoreSnapshotRequest.Mode.SAVE_THEN_RESTORE, true, GAMETEST_IDENTITY);
    if (!result.restored().equals(root)) {
      helper.fail("Save-then-restore reported an unexpected restored snapshot");
    }
    if (helper.getBlockState(probe) != original) {
      helper.fail("Save-then-restore did not rewind the world to the target snapshot");
    }

    var page = service.querySnapshotTree(parcel, 10, Optional.empty());
    if (page.nodes().size() != 2) {
      helper.fail("Save-then-restore must leave the base and the protective snapshot");
    }
    if (!page.current().orElseThrow().equals(root)) {
      helper.fail("Save-then-restore must end with the target as the current baseline");
    }
    boolean hasProtective =
        page.nodes().stream()
            .anyMatch(
                node ->
                    !node.id().equals(root) && node.name().startsWith("Before restore"));
    if (!hasProtective) {
      helper.fail("Save-then-restore did not retain a protective pre-restore snapshot");
    }
    if (!service.pendingRestoreOperations(parcel).isEmpty()) {
      helper.fail("Completed save-then-restore left a pending recovery operation");
    }

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /** Saving identical world content twice must still create a second, parented commit. */
  public void testIdenticalContentResaveStillCommits(GameTestHelpMore helper) throws Exception {
    var registry = ParcelRegistry.get(helper.getLevel());
    var service = SnapshotService.get(helper.getLevel());
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    var first = service.saveSnapshot(parcel, "First", "", GAMETEST_IDENTITY, true);
    var second = service.saveSnapshot(parcel, "Second", "", GAMETEST_IDENTITY, true);
    if (first.equals(second)) {
      helper.fail("Re-saving identical content must still create a new snapshot commit");
    }

    var page = service.querySnapshotTree(parcel, 10, Optional.empty());
    if (page.nodes().size() != 2) {
      helper.fail("Identical re-save must grow the snapshot history by one node");
    }
    var secondNode =
        page.nodes().stream()
            .filter(node -> node.id().equals(second))
            .findFirst()
            .orElseThrow();
    if (!secondNode.parentId().orElseThrow().equals(first)) {
      helper.fail("The re-saved snapshot must be a child of the first snapshot");
    }
    if (!page.current().orElseThrow().equals(second)) {
      helper.fail("The re-saved snapshot must become the current baseline");
    }

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /**
   * Entity round-trip contract: entities respawn with fresh UUIDs, passenger relations survive,
   * and intra-parcel entity references (the leash) are remapped to the fresh UUIDs so the link
   * survives restore.
   */
  public void testEntityRoundTripCharacteristics(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    var service = SnapshotService.get(level);
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    var cow = helper.spawn(GameEntityTypes.COW, new BlockPos(2, 1, 4));
    var holder = helper.spawn(GameEntityTypes.COW, new BlockPos(4, 1, 4));
    var chicken = helper.spawn(GameEntityTypes.CHICKEN, new BlockPos(2, 1, 4));
    chicken.startRiding(cow);
    cow.setLeashedTo(holder, true);
    if (!cow.isLeashed() || !chicken.isPassenger()) {
      helper.fail("Entity fixture must start leashed and riding");
    }
    var originalCowId = cow.getUUID();
    var originalHolderId = holder.getUUID();

    var snapshot = service.saveSnapshot(parcel, "Entities", "", GAMETEST_IDENTITY, false);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, false, GAMETEST_IDENTITY);

    var area = entityQueryArea(helper);
    var cows = level.getEntities(GameEntityTypes.COW, area, e -> true);
    var chickens = level.getEntities(GameEntityTypes.CHICKEN, area, e -> true);
    if (cows.size() != 2 || chickens.size() != 1) {
      helper.fail(
          "Restored parcel must contain exactly two cows and one chicken, got %d/%d"
              .formatted(cows.size(), chickens.size()));
    }
    if (cows.stream()
        .anyMatch(e -> e.getUUID().equals(originalCowId) || e.getUUID().equals(originalHolderId))) {
      helper.fail("Restored entities must receive fresh UUIDs");
    }
    var restoredChicken = chickens.getFirst();
    if (!restoredChicken.isPassenger() || !(restoredChicken.getVehicle() instanceof Cow)) {
      helper.fail("Restored chicken must still ride a cow");
    }

    helper
        .startSequence()
        .thenExecuteAfter(
            20,
            () -> {
              var leashed = level.getEntities(GameEntityTypes.COW, area, e -> e.isLeashed());
              if (leashed.size() != 1) {
                helper.fail(
                    "Exactly one cow must be leashed after reference remapping, got "
                        + leashed.size());
              }
              var leashHolder = leashed.getFirst().getLeashHolder();
              if (!(leashHolder instanceof Cow)) {
                helper.fail("The leash must still point at the other cow, got " + leashHolder);
              }
            })
        .thenSucceed();
  }

  /**
   * Resizing is a pure registration change: the anchor stays put, the next save captures the new
   * extent, a shrink leaves no stale section files behind, and restores fill whatever extent the
   * archive recorded.
   */
  public void testResizeRecapturesAdjustedExtent(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    var service = SnapshotService.get(level);
    registry.reset();

    var sourceBox = new BoundingBox(0, 0, 0, 5, 3, 5);
    var parcel =
        ParcelFactory.create(helper.absoluteBoundingBox(sourceBox), Mirror.NONE, Rotation.NONE);
    parcel.meta().setExcludeEntities(false);
    registry.addNewParcel(parcel);

    helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);

    var grownBox = new BoundingBox(0, 0, 0, 6, 6, 8);
    var anchorBefore = parcel.anchorPos();
    registry.resizeParcel(parcel, helper.absoluteBoundingBox(grownBox));
    if (!anchorBefore.equals(parcel.anchorPos())) {
      helper.fail("Resizing must not move the anchor");
    }
    if (!helper.absoluteBoundingBox(grownBox).equals(parcel.getBoundingBox())) {
      helper.fail("Resized parcel must cover exactly the requested box");
    }

    var snapshot = service.saveSnapshot(parcel, "Grown", "", GAMETEST_IDENTITY, true);
    fill(helper, helper.getRelativeBoundingBox(), Blocks.AIR.defaultBlockState());
    service.restoreSnapshot(parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, true, GAMETEST_IDENTITY);
    if (!helper.getBlockState(new BlockPos(6, 1, 6)).is(Blocks.STONE)) {
      helper.fail("The grown cell captured before the resize must survive restore");
    }

    var shrunkBox = new BoundingBox(0, 0, 0, 5, 3, 5);
    registry.resizeParcel(parcel, helper.absoluteBoundingBox(shrunkBox));
    helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(level, parcel, tempDir, true);
      fill(helper, helper.getRelativeBoundingBox(), Blocks.AIR.defaultBlockState());
      ParcelStorage.load(level, parcel.transform(), tempDir, false, true, WORLD_UPDATE_FLAGS);
    }
    if (!helper.getBlockState(new BlockPos(1, 1, 1)).is(Blocks.STONE)) {
      helper.fail("The shrunken parcel must keep loading its content after the stale sections");
    }

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /**
   * Two captures of an untouched world produce byte-identical trees (P3), and the eliminated
   * transient fields never enter the snapshot (definition 2.5).
   */
  public void testDeterministicCapture(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();
    var parcel =
        ParcelFactory.create(helper.absoluteBoundingBox(helper.getRelativeBoundingBox()),
            Mirror.NONE, Rotation.NONE);
    parcel.meta().setExcludeEntities(false);
    registry.addNewParcel(parcel);

    var cow = helper.spawn(GameEntityTypes.COW, new BlockPos(2, 1, 4));
    cow.setRemainingFireTicks(100);

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      var first = fs.getPath("/first");
      var second = fs.getPath("/second");
      ParcelStorage.captureSnapshot(level, parcel, first, false, ProgressReporter.NONE);
      ParcelStorage.captureSnapshot(level, parcel, second, false, ProgressReporter.NONE);

      var cowRecord = findEntityRecord(first);
      if (cowRecord.contains("\"Fire\"") || cowRecord.contains("\"HurtTime\"")) {
        helper.fail("Eliminated transient fields leaked into the snapshot: " + cowRecord);
      }
      if (cowRecord.isEmpty()) {
        helper.fail("The captured snapshot contains no entity record");
      }

      var mismatch = compareTrees(first, second);
      if (mismatch.isPresent()) {
        helper.fail("Repeated captures differ: " + mismatch.orElseThrow());
      }
    }

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /**
   * Scheduled block and fluid ticks round-trip with their trigger ticks re-anchored to the restore
   * game time (rule 2.3). Regional replacement (rule 6.3) supersedes a conflicting same-position
   * tick queued after the capture, keeps negative delays (already-elapsed targets), and leaves
   * ticks outside the extent untouched.
   */
  public void testScheduledTickRoundTrip(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    BlockPos insidePos = helper.absolutePos(new BlockPos(2, 1, 2));
    BlockPos pastPos = helper.absolutePos(new BlockPos(3, 1, 2));
    BlockPos fluidPos = helper.absolutePos(new BlockPos(4, 1, 2));
    BlockPos outsidePos = helper.absolutePos(new BlockPos(-3, 1, -3));

    long captureTime = level.getGameTime();
    blockTicks(level, insidePos)
        .schedule(
            new ScheduledTick<>(
                Blocks.REDSTONE_BLOCK, insidePos, captureTime + 100, TickPriority.NORMAL, 0L));
    blockTicks(level, pastPos)
        .schedule(
            new ScheduledTick<>(
                Blocks.REDSTONE_BLOCK, pastPos, captureTime - 50, TickPriority.HIGH, 0L));
    fluidTicks(level, fluidPos)
        .schedule(
            new ScheduledTick<>(
                Fluids.WATER, fluidPos, captureTime + 7, TickPriority.NORMAL, 0L));
    blockTicks(level, outsidePos)
        .schedule(
            new ScheduledTick<>(
                Blocks.REDSTONE_BLOCK, outsidePos, captureTime + 500, TickPriority.NORMAL, 0L));

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(level, parcel, tempDir, true);

      // Post-capture drift at the same position as a captured tick: the snapshot must win.
      blockTicks(level, insidePos).removeIf(t -> t.pos().equals(insidePos));
      blockTicks(level, insidePos)
          .schedule(
              new ScheduledTick<>(
                  Blocks.REDSTONE_BLOCK, insidePos, captureTime + 999, TickPriority.NORMAL, 0L));

      long restoreTime = level.getGameTime();
      ParcelStorage.load(level, parcel.transform(), tempDir, false, true, WORLD_UPDATE_FLAGS);

      assertTickTrigger(helper, level, "inside", restoreTime + 100, insidePos,
          Blocks.REDSTONE_BLOCK, false);
      assertTickTrigger(helper, level, "past", restoreTime - 50, pastPos,
          Blocks.REDSTONE_BLOCK, false);
      assertTickTrigger(helper, level, "fluid", restoreTime + 7, fluidPos, Fluids.WATER, true);
      assertTickTrigger(helper, level, "outside", captureTime + 500, outsidePos,
          Blocks.REDSTONE_BLOCK, false);
    }

    registry.deleteParcel(parcel.uuid());
    helper.succeed();
  }

  /**
   * Inside-pointing ticks travel with their target block through a rotated migration: the tick
   * lands on the restored marker block with its trigger re-anchored (invariants 3.1 and 6.3).
   */
  public void testScheduledTickRotatedMigration(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();

    var box = helper.getRelativeBoundingBox();
    int halfHeight = box.getYSpan() / 2;
    var sourceBox =
        new BoundingBox(
            box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY() + halfHeight - 1, box.maxZ());
    var targetBox =
        new BoundingBox(
            box.minX(),
            box.maxY() + 1 - halfHeight,
            box.minZ(),
            box.maxX(),
            box.maxY(),
            box.maxZ());

    var sourceParcel =
        ParcelFactory.create(helper.absoluteBoundingBox(sourceBox), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(sourceParcel);

    var markerRelative = new BlockPos(6, 1, 9);
    helper.setBlock(markerRelative, Blocks.GOLD_BLOCK);
    BlockPos markerPos = helper.absolutePos(markerRelative);
    long captureTime = level.getGameTime();
    blockTicks(level, markerPos)
        .schedule(
            new ScheduledTick<>(
                Blocks.GOLD_BLOCK, markerPos, captureTime + 33, TickPriority.NORMAL, 0L));

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(level, sourceParcel, tempDir, true);

      var targetParcel =
          ParcelFactory.create(
              helper.absoluteBoundingBox(targetBox), Mirror.NONE, Rotation.CLOCKWISE_90);
      long restoreTime = level.getGameTime();
      ParcelStorage.load(level, targetParcel.transform(), tempDir, false, true,
          WORLD_UPDATE_FLAGS);

      var targetArea = helper.absoluteBoundingBox(targetBox);
      BlockPos restoredMarker = null;
      for (BlockPos pos :
          BlockPos.betweenClosed(
              targetArea.minX(), targetArea.minY(), targetArea.minZ(),
              targetArea.maxX(), targetArea.maxY(), targetArea.maxZ())) {
        if (level.getBlockState(pos).is(Blocks.GOLD_BLOCK)) {
          restoredMarker = pos.immutable();
          break;
        }
      }
      if (restoredMarker == null) {
        helper.fail("Rotated parcel must restore the gold marker block");
      }
      assertTickTrigger(helper, level, "migrated", restoreTime + 33, restoredMarker,
          Blocks.GOLD_BLOCK, false);
    }

    registry.deleteParcel(sourceParcel.uuid());
    helper.succeed();
  }

  @SuppressWarnings("unchecked")
  private static LevelChunkTicks<Block> blockTicks(ServerLevel level, BlockPos pos) {
    return (LevelChunkTicks<Block>) level.getChunk(pos).getBlockTicks();
  }

  @SuppressWarnings("unchecked")
  private static LevelChunkTicks<Fluid> fluidTicks(ServerLevel level, BlockPos pos) {
    return (LevelChunkTicks<Fluid>) level.getChunk(pos).getFluidTicks();
  }

  private static <T> void assertTickTrigger(
      GameTestHelpMore helper,
      ServerLevel level,
      String label,
      long expected,
      BlockPos pos,
      T type,
      boolean fluid) {
    var container = fluid ? fluidTicks(level, pos) : blockTicks(level, pos);
    long actual =
        container.getAll()
            .filter(t -> t.pos().equals(pos) && t.type() == type)
            .findFirst()
            .map(ScheduledTick::triggerTick)
            .orElse(Long.MIN_VALUE);
    if (actual != expected) {
      helper.fail(label + " tick trigger must be " + expected + ", got " + actual);
    }
  }

  /** Reads the single entity record of a captured snapshot as text. */
  private static String findEntityRecord(Path snapshotRoot) throws Exception {
    try (var stream = Files.list(snapshotRoot.resolve("data/entities"))) {
      for (Path file : stream.filter(path -> path.toString().endsWith(".snbt")).toList()) {
        return Files.readString(file);
      }
    }
    return "";
  }

  /** Compares two trees file by file; returns the first mismatch description, if any. */
  private static java.util.Optional<String> compareTrees(Path first, Path second)
      throws Exception {
    try (var files = Files.walk(first);
        var others = Files.walk(second)) {
      var firstFiles = files.filter(Files::isRegularFile).sorted().toList();
      var secondFiles = others.filter(Files::isRegularFile).sorted().toList();
      if (firstFiles.size() != secondFiles.size()) {
        return java.util.Optional.of(
            "file count differs: %d vs %d".formatted(firstFiles.size(), secondFiles.size()));
      }
      for (int i = 0; i < firstFiles.size(); i++) {
        var relative = first.relativize(firstFiles.get(i));
        var otherRelative = second.relativize(secondFiles.get(i));
        if (!relative.equals(otherRelative)) {
          return java.util.Optional.of("path differs: %s vs %s".formatted(relative, otherRelative));
        }
        if (!java.util.Arrays.equals(Files.readAllBytes(firstFiles.get(i)),
            Files.readAllBytes(secondFiles.get(i)))) {
          return java.util.Optional.of("content differs: " + relative);
        }
      }
    }
    return java.util.Optional.empty();
  }

  /**
   * An item frame restored at a mirrored placement keeps its facing (direction role) and its
   * in-plane item rotation (45° step role), per SEMANTICS.md rule 3.1.
   */
  public void testItemFrameOrientationFollowsPlacement(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();

    var box = helper.getRelativeBoundingBox();
    int halfHeight = box.getYSpan() / 2;
    var sourceBox =
        new BoundingBox(
            box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY() + halfHeight - 1, box.maxZ());
    var targetBox =
        new BoundingBox(
            box.minX(),
            box.maxY() + 1 - halfHeight,
            box.minZ(),
            box.maxX(),
            box.maxY(),
            box.maxZ());

    var sourceParcel =
        ParcelFactory.create(helper.absoluteBoundingBox(sourceBox), Mirror.NONE, Rotation.NONE);
    sourceParcel.meta().setExcludeEntities(false);
    registry.addNewParcel(sourceParcel);

    var wallPos = new BlockPos(1, 1, 1);
    helper.setBlock(wallPos, Blocks.STONE);
    var frame = new ItemFrame(level, helper.absolutePos(wallPos), Direction.SOUTH);
    frame.setRotation(3);
    level.addFreshEntity(frame);

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, sourceParcel, tempDir, false);

      var targetParcel =
          ParcelFactory.create(helper.absoluteBoundingBox(targetBox), Mirror.LEFT_RIGHT, Rotation.NONE);
      targetParcel.meta().setExcludeEntities(false);
      ParcelStorage.load(
          level, targetParcel.transform(), tempDir, false, false, WORLD_UPDATE_FLAGS);
    }

    var targetArea =
        new AABB(
            Vec3.atCenterOf(
                helper.absolutePos(new BlockPos(0, targetBox.minY() - box.minY(), 0))),
            Vec3.atCenterOf(
                helper.absolutePos(
                    new BlockPos(
                        targetBox.getXSpan(),
                        targetBox.getYSpan() + (targetBox.minY() - box.minY()),
                        targetBox.getZSpan()))));
    var frames = level.getEntities(GameEntityTypes.ITEM_FRAME, targetArea, e -> true);
    if (frames.size() != 1) {
      helper.fail("Restored parcel must contain exactly one item frame, got " + frames.size());
    }
    var restored = frames.getFirst();
    if (restored.getDirection() != Direction.NORTH) {
      helper.fail(
          "Mirrored frame must face north, got " + restored.getDirection()); 
    }
    if (restored.getRotation() != 5) {
      helper.fail("Mirrored frame rotation must negate 3 to 5, got " + restored.getRotation());
    }

    registry.deleteParcel(sourceParcel.uuid());
    helper.succeed();
  }

  /**
   * Filled maps travel with their artwork: after a round trip the item points at a fresh map id
   * whose data equals the original, while the original map data stays untouched.
   */
  public void testMapItemCharacteristics(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    var service = SnapshotService.get(level);
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    var chestPos = new BlockPos(2, 0, 2);
    level.setBlock(
        helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
    var mapId = freshMapId(level);
    var mapData =
        MapItemSavedData.createFresh(0.5, 0.5, (byte) 0, false, true, Level.OVERWORLD);
    putMapData(level, mapId, mapData);
    var map = new ItemStack(Items.FILLED_MAP);
    setMapId(map, mapId);
    chest.setItem(0, map);

    var snapshot = service.saveSnapshot(parcel, "Maps", "", GAMETEST_IDENTITY, true);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, true, GAMETEST_IDENTITY);

    var restoredChest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
    var restoredMap = restoredChest.getItem(0);
    if (!restoredMap.is(Items.FILLED_MAP)) {
      helper.fail("Filled map must survive the snapshot round trip");
    }
    var restoredMapId = getMapId(restoredMap);
    if (restoredMapId == null) {
      helper.fail("Restored filled map must keep a map id");
    }
    if (restoredMapId.equals(mapId)) {
      helper.fail("Map item must receive a fresh map id through its attachment");
    }
    var restoredData = getMapData(level, restoredMapId);
    if (restoredData == null) {
      helper.fail("The map artwork must be copied into the level under the fresh id");
    }
    if (restoredData.centerX != mapData.centerX
        || restoredData.centerZ != mapData.centerZ
        || restoredData.scale != mapData.scale
        || restoredData.locked != mapData.locked
        || !java.util.Arrays.equals(restoredData.colors, mapData.colors)) {
      helper.fail("The copied map data must equal the original artwork");
    }
    if (getMapData(level, mapId) != mapData) {
      helper.fail("The original map data instance must remain untouched");
    }

    helper.succeed();
  }

  /**
   * Round-trips world-external data through the extension attachment channel: the game-test
   * extension collects a marker attachment during capture and restores it as a custom name.
   */
  public void testAttachmentRoundTrip(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    var service = SnapshotService.get(level);
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    helper.spawn(GameEntityTypes.COW, new BlockPos(2, 1, 4));

    var snapshot = service.saveSnapshot(parcel, "Attachment", "", GAMETEST_IDENTITY, false);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, false, GAMETEST_IDENTITY);

    var cows = level.getEntities(GameEntityTypes.COW, entityQueryArea(helper), e -> true);
    if (cows.size() != 1) {
      helper.fail("Restored parcel must contain exactly one cow, got " + cows.size());
    }
    var name = cows.getFirst().getCustomName();
    if (name == null || !MarkerRecordProcessor.MARKER_VALUE.equals(name.getString())) {
      helper.fail(
          "Restored cow must carry the marker custom name from the attachment, got " + name);
    }
    helper.succeed();
  }

  /**
   * Declared coordinate fields must follow the parcel: a beehive's flower position (root and
   * inside a stored bee occupant) is rebased when the snapshot is loaded into a parcel at a
   * different world position. Injected NBT uses era-correct key names so the test exercises the
   * vanilla serialization surface, not a parallel one.
   */
  public void testBeehiveFlowerPosFollowsParcel(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var box = helper.getRelativeBoundingBox();
    int halfHeight = box.getYSpan() / 2;
    var bottomBox =
        new BoundingBox(
            box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY() + halfHeight - 1, box.maxZ());
    var topBox =
        new BoundingBox(
            box.minX(),
            box.maxY() + 1 - halfHeight,
            box.minZ(),
            box.maxX(),
            box.maxY(),
            box.maxZ());

    var hivePos = new BlockPos(2, 0, 2);
    var flowerWorld = helper.absolutePos(new BlockPos(4, 1, 5));
    var nestedFlowerWorld = helper.absolutePos(new BlockPos(1, 1, 3));
    level.setBlock(helper.absolutePos(hivePos), Blocks.BEEHIVE.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var hive = (BeehiveBlockEntity) helper.getBlockEntity(hivePos);
    var hiveWorld = helper.absolutePos(hivePos);
    var injected = new CompoundTag();
    injected.putString("id", "minecraft:beehive");
    injected.putInt("x", hiveWorld.getX());
    injected.putInt("y", hiveWorld.getY());
    injected.putInt("z", hiveWorld.getZ());
    var nestedBee = new CompoundTag();
    nestedBee.putString("id", "minecraft:bee");
    var bees = new ListTag();
    var occupant = new CompoundTag();
    /*? if >=26.1 {*/
    injected.put(
        "flower_pos",
        BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, flowerWorld).result().orElseThrow());
    nestedBee.put(
        "flower_pos",
        BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, nestedFlowerWorld).result().orElseThrow());
    occupant.put("entity_data", nestedBee);
    occupant.putInt("ticks_in_hive", 1);
    occupant.putInt("min_ticks_in_hive", 1);
    bees.add(occupant);
    injected.put("bees", bees);
    try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      hive.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), injected));
    }
    /*?} else {*/
    /*injected.put("FlowerPos", blockPosCompound(flowerWorld));
    nestedBee.put("FlowerPos", blockPosCompound(nestedFlowerWorld));
    occupant.put("EntityData", nestedBee);
    occupant.putInt("TicksInHive", 1);
    occupant.putInt("MinOccupationTicks", 1);
    bees.add(occupant);
    injected.put("Bees", bees);
    hive.load(injected);
    *//*?}*/
    hive.setChanged();

    var source = ParcelFactory.create(helper.absoluteBoundingBox(bottomBox), Mirror.NONE, Rotation.NONE);
    var target = ParcelFactory.create(helper.absoluteBoundingBox(topBox), Mirror.NONE, Rotation.NONE);
    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/tmp");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, source, tempDir, true);
      ParcelStorage.load(
          level, target.transform(), tempDir, false, true,
          WORLD_UPDATE_FLAGS);
    }

    var restoredHive = (BeehiveBlockEntity) helper.getBlockEntity(hivePos.offset(0, halfHeight, 0));
    var restoredData = GameTestUtils.saveFullMetadata(level, restoredHive);
    /*? if >=26.1 {*/
    var restoredFlower =
        BlockPos.CODEC
            .parse(NbtOps.INSTANCE, restoredData.get("flower_pos"))
            .result()
            .orElseThrow(() -> new AssertionError("Restored beehive lost its flower_pos"));
    var restoredNested =
        BlockPos.CODEC
            .parse(
                NbtOps.INSTANCE,
                nestedBeeFlowerTag26(restoredData)
                    .orElseThrow(() -> new AssertionError("Restored beehive lost its stored bee")))
            .result()
            .orElseThrow(() -> new AssertionError("Stored bee lost its flower_pos"));
    /*?} else {*/
    /*var restoredFlower =
        readBlockPosCompound(
            restoredData.getCompound("FlowerPos"), "FlowerPos");
    var restoredNested =
        readBlockPosCompound(
            nestedBeeFlowerTag1201(restoredData)
                .orElseThrow(() -> new AssertionError("Restored beehive lost its stored bee"))
                .getCompound("EntityData")
                .getCompound("FlowerPos"),
            "stored bee FlowerPos");
    *//*?}*/
    var expectedFlower = flowerWorld.offset(0, halfHeight, 0);
    if (!expectedFlower.equals(restoredFlower)) {
      helper.fail(
          "flower_pos must follow the parcel: expected %s, got %s"
              .formatted(expectedFlower.toShortString(), restoredFlower.toShortString()));
    }
    var expectedNested = nestedFlowerWorld.offset(0, halfHeight, 0);
    if (!expectedNested.equals(restoredNested)) {
      helper.fail(
          "the stored bee's flower position must follow the parcel: expected %s, got %s"
              .formatted(expectedNested.toShortString(), restoredNested.toShortString()));
    }
    helper.succeed();
  }

  /**
   * Capture contributors round-trip world-external regional data: the contributor's capture hook
   * runs during save, its attachment travels in the snapshot, and its restore hook sees the payload
   * after the parcel content was applied.
   */
  public void testCaptureContributorRoundTrip(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    var service = SnapshotService.get(level);
    registry.reset();
    var parcel = ParcelFactory.create(helper.getBoundingBox(), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(parcel);

    RegionMarkerContributor.resetObservations();

    var snapshot = service.saveSnapshot(parcel, "Contributor", "", GAMETEST_IDENTITY, true);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, true, GAMETEST_IDENTITY);

    /*? if >=26.1 {*/
    var dimension = level.dimension().identifier().toString();
    /*?} else {*/
    /*var dimension = level.dimension().location().toString();
     *//*?}*/
    if (RegionMarkerContributor.restoreCalls != 1) {
      helper.fail(
          "Contributor restore hook must run exactly once, got "
              + RegionMarkerContributor.restoreCalls);
    }
    if (RegionMarkerContributor.restoredAttachmentCount != 1) {
      helper.fail(
          "Contributor must see exactly its own attachment, got "
              + RegionMarkerContributor.restoredAttachmentCount);
    }
    if (!dimension.equals(RegionMarkerContributor.restoredDimension)) {
      helper.fail(
          "Contributor payload must carry the capture-time dimension, got "
              + RegionMarkerContributor.restoredDimension);
    }
    helper.succeed();
  }

  private static AABB entityQueryArea(GameTestHelpMore helper) {
    var box = helper.getBoundingBox();
    return new AABB(
        box.minX() - 4,
        box.minY() - 4,
        box.minZ() - 4,
        box.maxX() + 5,
        box.maxY() + 5,
        box.maxZ() + 5);
  }

  private void doSaveAndLoad(
      GameTestHelpMore helper,
      Rotation rotation,
      Mirror mirror,
      BlockContentType.BlockSectionSize sectionSize)
      throws Exception {
    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
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

      var parcel =
          ParcelFactory.create(helper.absoluteBoundingBox(bottomBox), mirror, rotation);
      configureBlockSectionSize(parcel, sectionSize);
      ParcelStorage.save(helper.getLevel(), parcel, tempDir, true);

      var target = ParcelFactory.create(helper.absoluteBoundingBox(topBox), mirror, rotation);
      ParcelStorage.load(
          helper.getLevel(),
          target.transform(),
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

  private static void configureBlockSectionSize(
      io.github.leawind.gitparcel.common.api.world.Parcel parcel,
      BlockContentType.BlockSectionSize sectionSize) {
    var config = blockConfig(parcel);
    config.sectionSize.set(sectionSize);
    var contents = new java.util.LinkedHashMap<>(parcel.meta().contents());
    var previous = contents.get(BlockContentType.ID);
    contents.put(
        BlockContentType.ID,
        new io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentManifest(
            previous.version(), config.toJson()));
    parcel.meta().setContents(contents);
  }

  private static BlockContentType.Config blockConfig(
      io.github.leawind.gitparcel.common.api.world.Parcel parcel) {
    var manifest = parcel.meta().contents().get(BlockContentType.ID);
    if (manifest == null) {
      throw new IllegalStateException("Built-in blocks content is not registered");
    }
    var config = new BlockContentType.Config();
    if (manifest.config() != null) {
      config.setFromJson(manifest.config().getAsJsonObject());
    }
    return config;
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

  /** The cached archive metadata must describe the synced snapshot geometry after each sync. */
  private static void assertArchiveSyncRefreshed(
      GameTestHelpMore helper, Parcel parcel, String operation) {
    var sync = parcel.archiveSync().orElse(null);
    if (sync == null
        || !sync.size().equals(parcel.meta().size())
        || !sync.anchor().equals(parcel.meta().anchor())
        || sync.repositorySizeBytes() <= 0) {
      helper.fail("Archive sync cache was not refreshed after " + operation + ": " + sync);
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

  /*
   * Map ids are opaque across the version range: a MapId record on 26.x, a plain int in the item
   * tag before that. The helpers keep the tests version-neutral while the access shape lives in
   * one place.
   */
  private static Object freshMapId(ServerLevel level) {
    return level.getFreeMapId();
  }

  private static void putMapData(ServerLevel level, Object id, MapItemSavedData data) {
    /*? if >=26.1 {*/
    level.setMapData((MapId) id, data);
    /*?} else {*/
    /*level.setMapData("map_" + (Integer) id, data);
     *//*?}*/
  }

  private static MapItemSavedData getMapData(ServerLevel level, Object id) {
    /*? if >=26.1 {*/
    return level.getMapData((MapId) id);
    /*?} else {*/
    /*return level.getMapData("map_" + (Integer) id);
     *//*?}*/
  }

  private static void setMapId(ItemStack map, Object id) {
    /*? if >=26.1 {*/
    map.set(DataComponents.MAP_ID, (MapId) id);
    /*?} else {*/
    /*map.getOrCreateTag().putInt("map", (Integer) id);
     *//*?}*/
  }

  private static Object getMapId(ItemStack map) {
    /*? if >=26.1 {*/
    return map.get(DataComponents.MAP_ID);
    /*?} else {*/
    /*return map.hasTag() && map.getTag().contains("map") ? map.getTag().getInt("map") : null;
     *//*?}*/
  }

  /**
   * Paintings keep their hanging position and facing across a mirrored migration. 1.20.1 carries
   * the position in TileX/Y/Z (core processor) and the facing in the 3D-value Facing byte
   * (declared field); 26.x uses block_pos and the 2D-value facing key handled by its processor.
   */
  public void testPaintingFollowsPlacement(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(source);

    var wallPos = new BlockPos(2, 1, 2);
    helper.setBlock(wallPos, Blocks.STONE_BRICKS);
    /*? if >=26.1 {*/
    var variant =
        level
            .registryAccess()
            .lookupOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT)
            .getOrThrow(PaintingVariants.ALBAN);
    /*?} else {*/
    /*var variant =
        level
            .registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT)
            .getHolderOrThrow(PaintingVariants.ALBAN);
    *//*?}*/
    var painting =
        new Painting(level, helper.absolutePos(wallPos), Direction.SOUTH, variant);
    level.addFreshEntity(painting);

    var target =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.top()), Mirror.LEFT_RIGHT, Rotation.NONE);
    saveAndLoadAt(helper, level, source, target);

    var paintings =
        level.getEntities(GameEntityTypes.PAINTING, halves.targetArea(helper), e -> true);
    if (paintings.size() != 1) {
      helper.fail("Restored parcel must contain exactly one painting, got " + paintings.size());
    }
    var restored = paintings.getFirst();
    // Migration correctness (P2): the expected position is the source-relative position of the
    // painting mapped through the target placement, not the raw world coordinate.
    var targetSpace = new ParcelSpace(target.transform());
    var sourceSpace = new ParcelSpace(source.transform());
    var expectedPos = targetSpace.toWorld(sourceSpace.toParcel(helper.absolutePos(wallPos)));
    if (!restored.getPos().equals(expectedPos)) {
      helper.fail(
          "Painting must hang at the migrated position: expected %s, got %s"
              .formatted(expectedPos.toShortString(), restored.getPos().toShortString()));
    }
    var expectedFacing = targetSpace.toWorldDirection(Direction.SOUTH);
    if (restored.getDirection() != expectedFacing) {
      helper.fail(
          "Painting must follow the mirrored facing: expected %s, got %s"
              .formatted(expectedFacing, restored.getDirection()));
    }
    registry.deleteParcel(source.uuid());
    helper.succeed();
  }

  /**
   * Spatial edges relativize on capture: the leash coordinate form, the turtle home axes, and the
   * sleeping position all land in the snapshot as parcel-relative values, with era-correct key
   * names.
   */
  public void testSpatialEdgesSnapshotRelativization(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var registry = ParcelRegistry.get(level);
    registry.reset();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);
    registry.addNewParcel(source);
    var space = new ParcelSpace(source.transform());

    var fencePos = helper.absolutePos(new BlockPos(1, 1, 1));
    level.setBlock(fencePos, Blocks.OAK_FENCE.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var knot = new LeashFenceKnotEntity(level, fencePos);
    level.addFreshEntity(knot);
    var cow = helper.spawn(GameEntityTypes.COW, new BlockPos(3, 1, 3));
    cow.setLeashedTo(knot, true);

    var turtle = helper.spawn(GameEntityTypes.TURTLE, new BlockPos(5, 1, 5));
    turtle.setHomePos(helper.absolutePos(new BlockPos(2, 1, 4)));

    var bedPos = new BlockPos(3, 1, 2);
    level.setBlock(
        helper.absolutePos(bedPos), gametestBedBlock().defaultBlockState(), WORLD_UPDATE_FLAGS);
    var villager = helper.spawn(GameEntityTypes.VILLAGER, new BlockPos(4, 1, 2));
    /*? if >=26.3 {*/
    if (!villager.startSleeping(helper.absolutePos(bedPos))) {
      helper.fail("Villager must fall asleep on the bed before capture");
    }
    /*?} else {*/
    /*villager.startSleeping(helper.absolutePos(bedPos));
    *//*?}*/

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, source, tempDir, false);

      var knotLocal = space.toParcel(fencePos);
      var cowSnbt = findEntitySnbt(tempDir, "minecraft:cow");
      /*? if >=26.1 {*/
      assertSnbtContains(
          helper,
          cowSnbt,
          "leash:[I;"
              + knotLocal.getX() + "," + knotLocal.getY() + "," + knotLocal.getZ() + "]");
      /*?} else {*/
      /*assertSnbtInt(helper, cowSnbt, "X", knotLocal.getX());
      assertSnbtInt(helper, cowSnbt, "Y", knotLocal.getY());
      assertSnbtInt(helper, cowSnbt, "Z", knotLocal.getZ());
      *//*?}*/

      var turtleSnbt = findEntitySnbt(tempDir, "minecraft:turtle");
      var homeLocal = space.toParcel(helper.absolutePos(new BlockPos(2, 1, 4)));
      /*? if >=26.1 {*/
      assertSnbtContains(
          helper,
          turtleSnbt,
          "home_pos:[I;"
              + homeLocal.getX() + "," + homeLocal.getY() + "," + homeLocal.getZ() + "]");
      /*?} else {*/
      /*assertSnbtInt(helper, turtleSnbt, "HomePosX", homeLocal.getX());
      assertSnbtInt(helper, turtleSnbt, "HomePosY", homeLocal.getY());
      assertSnbtInt(helper, turtleSnbt, "HomePosZ", homeLocal.getZ());
      *//*?}*/

      var villagerSnbt = findEntitySnbt(tempDir, "minecraft:villager");
      var sleepLocal = space.toParcel(helper.absolutePos(new BlockPos(3, 1, 2)));
      /*? if >=26.1 {*/
      assertSnbtContains(
          helper,
          villagerSnbt,
          "sleeping_pos:[I;"
              + sleepLocal.getX() + "," + sleepLocal.getY() + "," + sleepLocal.getZ() + "]");
      /*?} else {*/
      /*assertSnbtInt(helper, villagerSnbt, "SleepingX", sleepLocal.getX());
      assertSnbtInt(helper, villagerSnbt, "SleepingY", sleepLocal.getY());
      assertSnbtInt(helper, villagerSnbt, "SleepingZ", sleepLocal.getZ());
      *//*?}*/
    }
    registry.deleteParcel(source.uuid());
    helper.succeed();
  }

  /**
   * The end gateway exit portal is a spatial edge. Structure-block posX/Y/Z, by contrast, are
   * offsets relative to the block itself (vanilla clamps them to +-48): they are payload, not
   * world coordinates, and must survive the migration untouched.
   */
  public void testEndGatewayAndStructureBlockFollowPlacement(GameTestHelpMore helper)
      throws Exception {
    var level = helper.getLevel();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);

    var gatewayPos = new BlockPos(1, 0, 1);
    level.setBlock(
        helper.absolutePos(gatewayPos), Blocks.END_GATEWAY.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var gateway = (TheEndGatewayBlockEntity) helper.getBlockEntity(gatewayPos);
    var exitWorld = helper.absolutePos(new BlockPos(3, 1, 4));
    gateway.setExitPosition(exitWorld, false);
    gateway.setChanged();

    var structPos = new BlockPos(5, 0, 1);
    level.setBlock(
        helper.absolutePos(structPos),
        Blocks.STRUCTURE_BLOCK.defaultBlockState(),
        WORLD_UPDATE_FLAGS);
    var structure = (StructureBlockEntity) helper.getBlockEntity(structPos);
    var originOffset = new BlockPos(-3, 2, 1);
    structure.setStructurePos(originOffset);
    structure.setChanged();

    saveAndLoadAt(helper, level, source, halves.plainTarget(helper));

    var restoredGateway =
        (TheEndGatewayBlockEntity)
            helper.getBlockEntity(gatewayPos.offset(0, halves.height(), 0));
    var gatewayData = GameTestUtils.saveFullMetadata(level, restoredGateway);
    /*? if >=26.1 {*/
    var restoredExit =
        BlockPos.CODEC
            .parse(NbtOps.INSTANCE, gatewayData.get("exit_portal"))
            .result()
            .orElseThrow(() -> new AssertionError("Restored gateway lost its exit_portal"));
    /*?} else {*/
    /*var restoredExit =
        readBlockPosCompound(
            NbtReads.getCompound(gatewayData, "ExitPortal"), "ExitPortal");
    *//*?}*/
    var expectedExit = exitWorld.offset(0, halves.height(), 0);
    if (!expectedExit.equals(restoredExit)) {
      helper.fail(
          "gateway exit must follow the parcel: expected %s, got %s"
              .formatted(expectedExit.toShortString(), restoredExit.toShortString()));
    }

    var restoredStructure =
        (StructureBlockEntity)
            helper.getBlockEntity(structPos.offset(0, halves.height(), 0));
    if (!restoredStructure.getStructurePos().equals(originOffset)) {
      helper.fail(
          "structure-block origin offset is payload and must survive untouched: expected "
              + originOffset
              + ", got "
              + restoredStructure.getStructurePos());
    }
    helper.succeed();
  }

  /**
   * Vibration-listener data relativizes on the three edges at once: the event position is a
   * spatial edge, the selector tick a game-time offset (returning to its stored value because
   * save and load run on the same tick), and the source UUID an identity edge kept verbatim.
   */
  public void testVibrationListenerRelativization(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);

    var sensorPos = new BlockPos(2, 0, 2);
    level.setBlock(
        helper.absolutePos(sensorPos),
        Blocks.SCULK_SENSOR.defaultBlockState(),
        WORLD_UPDATE_FLAGS);
    var sensor = (SculkSensorBlockEntity) helper.getBlockEntity(sensorPos);
    var eventPosWorld = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 1, 4)));
    var sourceId = UUID.randomUUID();
    var event = new CompoundTag();
    event.putString("game_event", "minecraft:block_open");
    event.putFloat("distance", 2.0F);
    event.put(
        "pos", Vec3.CODEC.encodeStart(NbtOps.INSTANCE, eventPosWorld).result().orElseThrow());
    event.put(
        "source", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, sourceId).result().orElseThrow());
    var selector = new CompoundTag();
    selector.put("event", event.copy());
    selector.putLong("tick", 12345L);
    var listener = new CompoundTag();
    listener.put("event", event);
    listener.put("selector", selector);
    listener.putInt("event_delay", 2);
    var injected = beBaseTag(helper, sensorPos, "minecraft:sculk_sensor");
    injected.put("listener", listener);
    loadBlockEntityNbt(helper, level, sensor, injected);
    sensor.setChanged();

    saveAndLoadAt(helper, level, source, halves.plainTarget(helper));

    var restoredSensor =
        (SculkSensorBlockEntity)
            helper.getBlockEntity(sensorPos.offset(0, halves.height(), 0));
    var restored = GameTestUtils.saveFullMetadata(level, restoredSensor);
    /*? if >=26.1 {*/
    if (!(restored.get("listener") instanceof CompoundTag listenerData)) {
      helper.fail("Restored sculk sensor lost its listener payload");
      return;
    }
    /*?} else {*/
    /*var listenerData = NbtReads.getCompound(restored, "listener");
    if (listenerData == null) {
      helper.fail("Restored sculk sensor lost its listener payload");
      return;
    }
    *//*?}*/
    var eventTag = NbtReads.getCompound(listenerData, "event");
    var selectorTag = NbtReads.getCompound(listenerData, "selector");
    if (eventTag == null || selectorTag == null) {
      helper.fail("Restored listener lost its event or selector payload");
      return;
    }
    var restoredPos =
        Vec3.CODEC
            .parse(NbtOps.INSTANCE, eventTag.get("pos"))
            .result()
            .orElseThrow(() -> new AssertionError("Restored event lost its pos"));
    var expectedPos = eventPosWorld.add(0, halves.height(), 0);
    if (!restoredPos.equals(expectedPos)) {
      helper.fail(
          "listener event pos must follow the parcel: expected %s, got %s"
              .formatted(expectedPos, restoredPos));
    }
    var restoredTick =
        NbtReads.read(selectorTag, "tick", Codec.LONG).orElse(Long.MIN_VALUE);
    if (restoredTick != 12345L) {
      helper.fail(
          "listener selector tick must re-anchor to the same stored offset, got " + restoredTick);
    }
    var restoredSource =
        UUIDUtil.CODEC
            .parse(NbtOps.INSTANCE, eventTag.get("source"))
            .result()
            .orElseThrow(() -> new AssertionError("Restored event lost its source"));
    if (!restoredSource.equals(sourceId)) {
      helper.fail("listener source uuid must be kept verbatim");
    }
    helper.succeed();
  }

  /**
   * Identity edges inside the batch are rewritten: the arrow's owner UUID points at the restored
   * skeleton, and the shulker's attach face survives the mirrored round trip.
   */
  public void testIdentityEdgesFollowBatchRewrite(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);

    var skeleton = helper.spawn(GameEntityTypes.SKELETON, new BlockPos(2, 1, 2));
    var arrow = helper.spawn(GameEntityTypes.ARROW, new BlockPos(3, 1, 3));
    arrow.setOwner(skeleton);
    var shulker = helper.spawn(GameEntityTypes.SHULKER, new BlockPos(5, 1, 5));
    var originalAttachFace = shulker.getAttachFace();

    var target =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.top()), Mirror.FRONT_BACK, Rotation.NONE);
    saveAndLoadAt(helper, level, source, target);

    var area = halves.targetArea(helper);
    var arrows = level.getEntities(GameEntityTypes.ARROW, area, e -> true);
    var skeletons = level.getEntities(GameEntityTypes.SKELETON, area, e -> true);
    if (arrows.size() != 1 || skeletons.size() != 1) {
      helper.fail(
          "Restored parcel must contain one arrow and one skeleton, got "
              + arrows.size()
              + "/"
              + skeletons.size());
    }
    var owner = arrows.getFirst().getOwner();
    if (owner == null || !owner.getUUID().equals(skeletons.getFirst().getUUID())) {
      helper.fail("Arrow owner must be rewritten to the restored skeleton");
    }
    var shulkers = level.getEntities(GameEntityTypes.SHULKER, area, e -> true);
    if (shulkers.size() != 1) {
      helper.fail("Restored parcel must contain one shulker, got " + shulkers.size());
    }
    var expectedFace = new ParcelSpace(target.transform()).toWorldDirection(originalAttachFace);
    if (shulkers.getFirst().getAttachFace() != expectedFace) {
      helper.fail(
          "Shulker attach face must follow the mirror: expected %s, got %s"
              .formatted(expectedFace, shulkers.getFirst().getAttachFace()));
    }
    helper.succeed();
  }

  /**
   * Embedded item data travels with the parcel: a filled map inside a villager's inventory gets a
   * fresh map id through its attachment, and lodestone compasses rebase their inside-pointing
   * lodestone while keeping the outside-pointing one identical.
   */
  public void testEmbeddedItemsTravelWithParcel(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);

    var mapId = freshMapId(level);
    var mapData = MapItemSavedData.createFresh(0.5, 0.5, (byte) 0, false, true, Level.OVERWORLD);
    putMapData(level, mapId, mapData);
    var map = new ItemStack(Items.FILLED_MAP);
    setMapId(map, mapId);
    var villager = helper.spawn(GameEntityTypes.VILLAGER, new BlockPos(2, 1, 2));
    villager.getInventory().setItem(0, map);

    var chestPos = new BlockPos(4, 0, 4);
    level.setBlock(
        helper.absolutePos(chestPos), Blocks.CHEST.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
    var insideLodestone = helper.absolutePos(new BlockPos(1, 1, 1));
    var outsideLodestone = new BlockPos(9000, 64, 9000);
    chest.setItem(0, lodestoneCompass(insideLodestone));
    chest.setItem(1, lodestoneCompass(outsideLodestone));
    chest.setChanged();

    saveAndLoadAt(helper, level, source, halves.plainTarget(helper));

    var restoredVillager =
        level
            .getEntities(GameEntityTypes.VILLAGER, halves.targetArea(helper), e -> true)
            .getFirst();
    var carried = restoredVillager.getInventory().getItem(0);
    var newMapId = getMapId(carried);
    if (newMapId == null || newMapId.equals(mapId)) {
      helper.fail("The map in the villager inventory must receive a fresh map id");
    }
    if (getMapData(level, newMapId) == null) {
      helper.fail("The fresh map id must resolve to restored map data");
    }

    var restoredChest =
        (ChestBlockEntity) helper.getBlockEntity(chestPos.offset(0, halves.height(), 0));
    var restoredInside = lodestoneOf(restoredChest.getItem(0));
    var restoredOutside = lodestoneOf(restoredChest.getItem(1));
    var expectedInside = insideLodestone.offset(0, halves.height(), 0);
    if (!expectedInside.equals(restoredInside)) {
      helper.fail(
          "The inside-pointing lodestone must follow the parcel: expected %s, got %s"
              .formatted(expectedInside, restoredInside));
    }
    if (!outsideLodestone.equals(restoredOutside)) {
      helper.fail(
          "The outside-pointing lodestone must stay identical: expected %s, got %s"
              .formatted(outsideLodestone, restoredOutside));
    }
    helper.succeed();
  }

  /**
   * Time-noise fields never enter snapshots: jukebox playback progress, the hopper transfer
   * cooldown, and the spawner delay are eliminated on capture.
   */
  public void testNoiseFieldsEliminatedOnCapture(GameTestHelpMore helper) throws Exception {
    var level = helper.getLevel();
    var halves = verticalHalves(helper);
    var source =
        ParcelFactory.create(
            helper.absoluteBoundingBox(halves.bottom()), Mirror.NONE, Rotation.NONE);

    var jukeboxPos = new BlockPos(1, 0, 1);
    level.setBlock(
        helper.absolutePos(jukeboxPos), Blocks.JUKEBOX.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var jukebox = (JukeboxBlockEntity) helper.getBlockEntity(jukeboxPos);
    var jukeboxTag = beBaseTag(helper, jukeboxPos, "minecraft:jukebox");
    /*? if >=26.1 {*/
    jukeboxTag.putLong("ticks_since_song_started", 99L);
    /*?} else {*/
    /*jukeboxTag.putBoolean("IsPlaying", true);
    jukeboxTag.putLong("RecordStartTick", 5L);
    jukeboxTag.putLong("TickCount", 99L);
    *//*?}*/
    loadBlockEntityNbt(helper, level, jukebox, jukeboxTag);
    jukebox.setChanged();

    var hopperPos = new BlockPos(2, 0, 2);
    level.setBlock(
        helper.absolutePos(hopperPos), Blocks.HOPPER.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var hopper = (HopperBlockEntity) helper.getBlockEntity(hopperPos);
    var hopperTag = beBaseTag(helper, hopperPos, "minecraft:hopper");
    hopperTag.putInt("TransferCooldown", 8);
    loadBlockEntityNbt(helper, level, hopper, hopperTag);
    hopper.setChanged();

    var spawnerPos = new BlockPos(3, 0, 3);
    level.setBlock(
        helper.absolutePos(spawnerPos), Blocks.SPAWNER.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var spawner = (SpawnerBlockEntity) helper.getBlockEntity(spawnerPos);
    var spawnerTag = beBaseTag(helper, spawnerPos, "minecraft:spawner");
    spawnerTag.putShort("Delay", (short) 5);
    loadBlockEntityNbt(helper, level, spawner, spawnerTag);
    spawner.setChanged();

    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, source, tempDir, true);

      var jukeboxSnbt = findBlockEntitySnbt(tempDir, "minecraft:jukebox");
      /*? if >=26.1 {*/
      assertSnbtAbsent(helper, jukeboxSnbt, "ticks_since_song_started");
      /*?} else {*/
      /*assertSnbtAbsent(helper, jukeboxSnbt, "IsPlaying");
      assertSnbtAbsent(helper, jukeboxSnbt, "RecordStartTick");
      assertSnbtAbsent(helper, jukeboxSnbt, "TickCount");
      *//*?}*/
      var hopperSnbt = findBlockEntitySnbt(tempDir, "minecraft:hopper");
      assertSnbtAbsent(helper, hopperSnbt, "TransferCooldown");
      var spawnerSnpt = findBlockEntitySnbt(tempDir, "minecraft:mob_spawner");
      assertSnbtAbsent(helper, spawnerSnpt, "Delay");
    }
    helper.succeed();
  }

  private record BlockSnapshot(List<BlockPos> positions, List<BlockState> states) {
    private BlockSnapshot {
      positions = List.copyOf(positions);
      states = List.copyOf(states);
    }
  }
  /*? if >=26.1 {*/
  private static final String BEES_KEY = "bees";
  private static final String OCCUPANT_ENTITY_KEY = "entity_data";
  private static final String OCCUPANT_FLOWER_KEY = "flower_pos";
  /*?} else {*/
  /*private static final String BEES_KEY = "Bees";
  private static final String OCCUPANT_ENTITY_KEY = "EntityData";
  private static final String OCCUPANT_FLOWER_KEY = "FlowerPos";
  *//*?}*/

  /** The two vertical halves of the template box, a common migration fixture. */
  private record VerticalHalves(BoundingBox bottom, BoundingBox top, int height) {
    private Parcel plainTarget(GameTestHelpMore helper) {
      return ParcelFactory.create(
          helper.absoluteBoundingBox(top), Mirror.NONE, Rotation.NONE);
    }

    private AABB targetArea(GameTestHelpMore helper) {
      var box = helper.getRelativeBoundingBox();
      return new AABB(
          Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, top.minY() - box.minY(), 0))),
          Vec3.atCenterOf(
              helper.absolutePos(
                  new BlockPos(
                      top.getXSpan(),
                      top.getYSpan() + (top.minY() - box.minY()),
                      top.getZSpan()))));
    }
  }

  private static VerticalHalves verticalHalves(GameTestHelpMore helper) {
    var box = helper.getRelativeBoundingBox();
    int halfHeight = box.getYSpan() / 2;
    return new VerticalHalves(
        new BoundingBox(
            box.minX(), box.minY(), box.minZ(), box.maxX(), box.minY() + halfHeight - 1, box.maxZ()),
        new BoundingBox(
            box.minX(),
            box.maxY() + 1 - halfHeight,
            box.minZ(),
            box.maxX(),
            box.maxY(),
            box.maxZ()),
        halfHeight);
  }

  /** Saves entity payload NBT through the era's save entry point. */
  private static CompoundTag saveEntityNbt(
      ServerLevel level, net.minecraft.world.entity.Entity entity) {
    /*? if >=26.1 {*/
    try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
      entity.saveWithoutId(output);
      return output.buildResult();
    }
    /*?} else {*/
    /*var tag = new CompoundTag();
    entity.saveWithoutId(tag);
    return tag;
    *//*?}*/
  }

  /*? if >=26.3 {*/
  private static net.minecraft.world.level.block.Block gametestBedBlock() {
    return Blocks.STRAW_BED;
  }
  /*?} else {*/
  /*private static net.minecraft.world.level.block.Block gametestBedBlock() {
    return Blocks.RED_BED;
  }
  *//*?}*/

  /** Saves the source parcel into a fresh in-memory tree and loads it at the target placement. */
  private static void saveAndLoadAt(
      GameTestHelpMore helper, ServerLevel level, Parcel source, Parcel target) throws Exception {
    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      Path tempDir = fs.getPath("/parcel");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, source, tempDir, false);
      ParcelStorage.load(
          level, target.transform(), tempDir, false, false, WORLD_UPDATE_FLAGS);
    }
  }

  /** A base tag for injecting block-entity NBT: id plus world x/y/z. */
  private static CompoundTag beBaseTag(GameTestHelpMore helper, BlockPos relativePos, String id) {
    var world = helper.absolutePos(relativePos);
    var tag = new CompoundTag();
    tag.putString("id", id);
    tag.putInt("x", world.getX());
    tag.putInt("y", world.getY());
    tag.putInt("z", world.getZ());
    return tag;
  }

  /** Loads injected NBT into a placed block entity through the era's load entry point. */
  private static void loadBlockEntityNbt(
      GameTestHelpMore helper, ServerLevel level, BlockEntity be, CompoundTag tag) {
    /*? if >=26.1 {*/
    try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      be.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), tag));
    }
    /*?} else {*/
    /*be.load(tag);
    *//*?}*/
  }

  /** Writes a legacy {X,Y,Z} block-position compound (NbtUtils.writeBlockPos form). */
  private static CompoundTag blockPosCompound(BlockPos pos) {
    var compound = new CompoundTag();
    compound.putInt("X", pos.getX());
    compound.putInt("Y", pos.getY());
    compound.putInt("Z", pos.getZ());
    return compound;
  }

  private static BlockPos readBlockPosCompound(CompoundTag compound, String label) {
    Integer x = NbtReads.getIntOrNull(compound, "X");
    Integer y = NbtReads.getIntOrNull(compound, "Y");
    Integer z = NbtReads.getIntOrNull(compound, "Z");
    if (x == null || y == null || z == null) {
      throw new AssertionError("Missing " + label + " axes");
    }
    return new BlockPos(x, y, z);
  }

  private static java.util.Optional<CompoundTag> firstStoredBee(CompoundTag hiveData) {
    var bees = NbtReads.getList(hiveData, BEES_KEY);
    if (bees == null) {
      return java.util.Optional.empty();
    }
    for (var element : bees) {
      if (element instanceof CompoundTag occupant) {
        return java.util.Optional.of(occupant);
      }
    }
    return java.util.Optional.empty();
  }

  /*? if >=26.1 {*/
  private static java.util.Optional<Tag> nestedBeeFlowerTag26(CompoundTag hiveData) {
    var bee = firstStoredBee(hiveData).orElse(null);
    if (bee == null) {
      return java.util.Optional.empty();
    }
    var entityData = NbtReads.getCompound(bee, OCCUPANT_ENTITY_KEY);
    if (entityData == null) {
      return java.util.Optional.empty();
    }
    var flower = entityData.get(OCCUPANT_FLOWER_KEY);
    return flower == null ? java.util.Optional.empty() : java.util.Optional.of(flower);
  }
  /*?} else {*/
  /*private static java.util.Optional<CompoundTag> nestedBeeFlowerTag1201(CompoundTag hiveData) {
    var bee = firstStoredBee(hiveData).orElse(null);
    if (bee == null) {
      return java.util.Optional.empty();
    }
    var entityData = NbtReads.getCompound(bee, OCCUPANT_ENTITY_KEY);
    if (entityData == null) {
      return java.util.Optional.empty();
    }
    var flower = NbtReads.getCompound(entityData, OCCUPANT_FLOWER_KEY);
    return flower == null ? java.util.Optional.empty() : java.util.Optional.of(flower);
  }
  *//*?}*/

  /** Finds the entity snapshot record carrying the given type id, as snbt text. */
  private static String findEntitySnbt(Path snapshotRoot, String entityId) throws Exception {
    return findSnbt(snapshotRoot.resolve("data/entities"), entityId);
  }

  /** Finds the block-entity snapshot record carrying the given type id, as snbt text. */
  private static String findBlockEntitySnbt(Path snapshotRoot, String beTypeId) throws Exception {
    return findSnbt(snapshotRoot.resolve("data/blocks"), beTypeId);
  }

  private static String findSnbt(Path directory, String typeId) throws Exception {
    String needle = "\"" + typeId + "\"";
    try (var stream = Files.walk(directory)) {
      for (Path file : stream.filter(path -> path.toString().endsWith(".snbt")).toList()) {
        var text = Files.readString(file);
        if (text.contains(typeId) || text.contains(needle)) {
          // Snapshot snbt is pretty-printed; tokens match on whitespace-stripped text.
          return text.replaceAll("\\s+", "");
        }
      }
    }
    throw new AssertionError("No snapshot record found for " + typeId);
  }

  /** Asserts an int value appears under the exact key in snbt text. */
  private static void assertSnbtInt(GameTestHelpMore helper, String snbt, String key, int value) {
    var pattern = java.util.regex.Pattern.compile(key + ":" + value + "(?=[,}])");
    if (!pattern.matcher(snbt).find()) {
      helper.fail("Expected " + key + ":" + value + " in snapshot record");
    }
  }

  private static void assertSnbtContains(GameTestHelpMore helper, String snbt, String token) {
    if (!snbt.contains(token)) {
      helper.fail("Expected token '" + token + "' in snapshot record");
    }
  }

  /** Asserts a key is absent, with word boundaries so MinSpawnDelay does not match Delay. */
  private static void assertSnbtAbsent(GameTestHelpMore helper, String snbt, String key) {
    var pattern = java.util.regex.Pattern.compile("\\b" + key + ":");
    if (pattern.matcher(snbt).find()) {
      helper.fail("Eliminated key '" + key + "' must not appear in the snapshot record");
    }
  }

  /** Builds a lodestone compass pointing at the given position, in the era's item form. */
  private static ItemStack lodestoneCompass(BlockPos pos) {
    var stack = new ItemStack(Items.COMPASS);
    /*? if >=26.1 {*/
    stack.set(
        DataComponents.LODESTONE_TRACKER,
        new LodestoneTracker(java.util.Optional.of(GlobalPos.of(Level.OVERWORLD, pos)), true));
    return stack;
    /*?} else {*/
    /*var tag = stack.getOrCreateTag();
    tag.put("LodestonePos", blockPosCompound(pos));
    tag.putString("LodestoneDimension", "minecraft:overworld");
    tag.putBoolean("LodestoneTracked", true);
    return stack;
    *//*?}*/
  }

  /*? if >=26.1 {*/
  private static BlockPos lodestoneOf(ItemStack stack) {
    var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
    return tracker == null ? null : tracker.target().map(GlobalPos::pos).orElse(null);
  }
  /*?} else {*/
  /*private static BlockPos lodestoneOf(ItemStack stack) {
    var tag = stack.getTag();
    if (tag == null || !tag.contains("LodestonePos", 10)) {
      return null;
    }
    return readBlockPosCompound(tag.getCompound("LodestonePos"), "LodestonePos");
  }
  *//*?}*/
}
