package io.github.leawind.gitparcel.gametest;

import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import io.github.leawind.gitparcel.server.minecraft.logic.world.SnapshotService;
import com.google.common.jimfs.Jimfs;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.common.api.snapshot.RestoreSnapshotRequest;
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
import io.github.leawind.gitparcel.gametest.utils.GameTestHelpMore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
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
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
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

    try (var fs = Jimfs.newFileSystem()) {
      Path tempDir = fs.getPath("/parcel");
      ParcelStorage.save(helper.getLevel(), parcel, tempDir, true);

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

    var cow = helper.spawn(EntityType.COW, new BlockPos(2, 1, 4));
    var holder = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
    var chicken = helper.spawn(EntityType.CHICKEN, new BlockPos(2, 1, 4));
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
    var cows = level.getEntities(EntityType.COW, area, e -> true);
    var chickens = level.getEntities(EntityType.CHICKEN, area, e -> true);
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
              var leashed = level.getEntities(EntityType.COW, area, Leashable::isLeashed);
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
    var mapId = level.getFreeMapId();
    var mapData =
        MapItemSavedData.createFresh(0.5, 0.5, (byte) 0, false, true, Level.OVERWORLD);
    level.setMapData(mapId, mapData);
    var map = new ItemStack(Items.FILLED_MAP);
    map.set(DataComponents.MAP_ID, mapId);
    chest.setItem(0, map);

    var snapshot = service.saveSnapshot(parcel, "Maps", "", GAMETEST_IDENTITY, true);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, true, GAMETEST_IDENTITY);

    var restoredChest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
    var restoredMap = restoredChest.getItem(0);
    if (!restoredMap.is(Items.FILLED_MAP)) {
      helper.fail("Filled map must survive the snapshot round trip");
    }
    var restoredMapId = restoredMap.get(DataComponents.MAP_ID);
    if (restoredMapId == null) {
      helper.fail("Restored filled map must keep a map id component");
    }
    if (restoredMapId.equals(mapId)) {
      helper.fail("Map item must receive a fresh map id through its attachment");
    }
    var restoredData = level.getMapData(restoredMapId);
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
    if (level.getMapData(mapId) != mapData) {
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

    helper.spawn(EntityType.COW, new BlockPos(2, 1, 4));

    var snapshot = service.saveSnapshot(parcel, "Attachment", "", GAMETEST_IDENTITY, false);
    service.restoreSnapshot(
        parcel, snapshot, RestoreSnapshotRequest.Mode.DIRECT, false, GAMETEST_IDENTITY);

    var cows = level.getEntities(EntityType.COW, entityQueryArea(helper), e -> true);
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
   * Declared coordinate fields must follow the parcel: a beehive's {@code flower_pos} is rebased
   * when the snapshot is loaded into a parcel at a different world position.
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
    level.setBlock(helper.absolutePos(hivePos), Blocks.BEEHIVE.defaultBlockState(), WORLD_UPDATE_FLAGS);
    var hive = (BeehiveBlockEntity) helper.getBlockEntity(hivePos);
    var hiveWorld = helper.absolutePos(hivePos);
    var injected = new CompoundTag();
    injected.putString("id", "minecraft:beehive");
    injected.putInt("x", hiveWorld.getX());
    injected.putInt("y", hiveWorld.getY());
    injected.putInt("z", hiveWorld.getZ());
    injected.put(
        "flower_pos", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, flowerWorld).getOrThrow());
    injected.put("bees", new ListTag());
    try (var reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      hive.loadWithComponents(TagValueInput.create(reporter, level.registryAccess(), injected));
    }
    hive.setChanged();

    var source = ParcelFactory.create(helper.absoluteBoundingBox(bottomBox), Mirror.NONE, Rotation.NONE);
    var target = ParcelFactory.create(helper.absoluteBoundingBox(topBox), Mirror.NONE, Rotation.NONE);
    try (var fs = Jimfs.newFileSystem()) {
      Path tempDir = fs.getPath("/tmp");
      Files.createDirectories(tempDir);
      ParcelStorage.save(level, source, tempDir, true);
      ParcelStorage.load(
          level, target.transform(), tempDir, false, true,
          WORLD_UPDATE_FLAGS);
    }

    var restoredHive = (BeehiveBlockEntity) helper.getBlockEntity(hivePos.offset(0, halfHeight, 0));
    var restoredData = restoredHive.saveWithFullMetadata(level.registryAccess());
    var restoredFlower =
        BlockPos.CODEC
            .parse(NbtOps.INSTANCE, restoredData.get("flower_pos"))
            .result()
            .orElseThrow(() -> new AssertionError("Restored beehive lost its flower_pos"));
    var expectedFlower = flowerWorld.offset(0, halfHeight, 0);
    if (!expectedFlower.equals(restoredFlower)) {
      helper.fail(
          "flower_pos must follow the parcel: expected %s, got %s"
              .formatted(expectedFlower.toShortString(), restoredFlower.toShortString()));
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

    var dimension = level.dimension().identifier().toString();
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
