package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.BlockPalette;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.Subparcel;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.SubparcelFormat;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Format;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParcellaTest extends AbstractMinecraftTest {
  private static final Subparcel TEST_SUBPARCEL = new Subparcel(0, 0, 0, 2, 2, 2);
  private static final Logger LOGGER = LoggerFactory.getLogger(ParcellaTest.class);
  private static final ProblemReporter DUMMY_REPORTER = new ProblemReporter.ScopedCollector(LOGGER);

  private ParcellaD32Loader.Context paletteCtx;

  @BeforeEach
  void setUpParcella() {
    var paletteConfig = new ParcellaD32Format.Config();
    paletteConfig.usePalette.set(true);
    paletteCtx =
        new ParcellaD32Loader.Context(
            null, null, null, null, Path.of(""), false, false, 0, paletteConfig);
    paletteCtx.blockPalette = new BlockPalette();
    paletteCtx.blockPalette.collect(Blocks.AIR.defaultBlockState());
    paletteCtx.blockPalette.collect(Blocks.STONE.defaultBlockState());
    paletteCtx.blockPalette.collect(Blocks.DIRT.defaultBlockState());
    paletteCtx.blockPalette.collect(Blocks.COBBLESTONE.defaultBlockState());
  }

  @Test
  void detectSubparcelFormat() {
    byte[] rle3dPaletteData = "000~000\n1\n".getBytes();
    Assertions.assertEquals(
        SubparcelFormat.RLE3D, ParcellaD32Loader.detectSubparcelFormat(rle3dPaletteData));

    byte[] rle3dInlineData = "000=minecraft:stone\n1\n".getBytes();
    Assertions.assertEquals(
        SubparcelFormat.RLE3D, ParcellaD32Loader.detectSubparcelFormat(rle3dInlineData));

    byte[] flatData = "1\n2\n3\n4\n5\n6\n7\n8\n".getBytes();
    assertEquals(SubparcelFormat.FLAT, ParcellaD32Loader.detectSubparcelFormat(flatData));

    byte[] shortData = "123".getBytes();
    assertEquals(SubparcelFormat.FLAT, ParcellaD32Loader.detectSubparcelFormat(shortData));
  }

  @Test
  void loadSubparcelRLE3DWithPalette() {
    byte[] data = "000~0\n001~1\n".getBytes();

    List<int[]> positions = new ArrayList<>();
    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesRLE3D(
            paletteCtx,
            data,
            (x, y, z, blockState) -> {
              positions.add(new int[] {x, y, z});
              blockStates.add(blockState);
            },
            DUMMY_REPORTER);

    assertEquals(2, blockStates.size());
    assertArrayEquals(new int[] {0, 0, 0}, positions.get(0));
    assertEquals(Blocks.AIR.defaultBlockState(), blockStates.get(0));
    assertArrayEquals(new int[] {0, 0, 1}, positions.get(1));
    assertEquals(Blocks.STONE.defaultBlockState(), blockStates.get(1));
  }

  @Test
  void loadSubparcelRLE3DInline() {
    byte[] data = "000=minecraft:air\n001=minecraft:stone\n".getBytes();

    var noPaletteConfig = new ParcellaD32Format.Config();
    noPaletteConfig.usePalette.set(false);
    var noPaletteCtx =
        new ParcellaD32Loader.Context(
            null, null, null, null, Path.of(""), false, false, 0, noPaletteConfig);

    List<int[]> positions = new ArrayList<>();
    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesRLE3D(
            noPaletteCtx,
            data,
            (x, y, z, blockState) -> {
              positions.add(new int[] {x, y, z});
              blockStates.add(blockState);
            },
            DUMMY_REPORTER);

    assertEquals(2, blockStates.size());
    assertArrayEquals(new int[] {0, 0, 0}, positions.get(0));
    assertEquals(Blocks.AIR.defaultBlockState(), blockStates.get(0));
    assertArrayEquals(new int[] {0, 0, 1}, positions.get(1));
    assertEquals(Blocks.STONE.defaultBlockState(), blockStates.get(1));
  }

  @Test
  void loadSubparcelRLE3DRange() {
    byte[] data = "000111~3\n".getBytes();

    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesRLE3D(
            paletteCtx, data, (x, y, z, blockState) -> blockStates.add(blockState), DUMMY_REPORTER);

    assertEquals(8, blockStates.size());
    for (BlockState bs : blockStates) {
      assertEquals(Blocks.COBBLESTONE.defaultBlockState(), bs);
    }
  }

  @Test
  void loadSubparcelFLAT() {
    byte[] data = "0\n1\n2\n3\n0\n1\n2\n3\n".getBytes();

    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesFLAT(
            paletteCtx,
            TEST_SUBPARCEL,
            data,
            (x, y, z, blockState) -> blockStates.add(blockState),
            DUMMY_REPORTER);

    assertEquals(8, blockStates.size());
    assertEquals(Blocks.AIR.defaultBlockState(), blockStates.get(0));
    assertEquals(Blocks.STONE.defaultBlockState(), blockStates.get(1));
    assertEquals(Blocks.DIRT.defaultBlockState(), blockStates.get(2));
    assertEquals(Blocks.COBBLESTONE.defaultBlockState(), blockStates.get(3));
    assertEquals(Blocks.AIR.defaultBlockState(), blockStates.get(4));
    assertEquals(Blocks.STONE.defaultBlockState(), blockStates.get(5));
    assertEquals(Blocks.DIRT.defaultBlockState(), blockStates.get(6));
    assertEquals(Blocks.COBBLESTONE.defaultBlockState(), blockStates.get(7));
  }

  @Test
  void loadSubparcelRLE3DPaletteIdWithoutPalette() {
    byte[] data = "000~0\n".getBytes();

    var noPaletteConfig = new ParcellaD32Format.Config();
    noPaletteConfig.usePalette.set(false);
    var noPaletteCtx =
        new ParcellaD32Loader.Context(
            null, null, null, null, Path.of(""), false, false, 0, noPaletteConfig);

    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesRLE3D(
            noPaletteCtx,
            data,
            (x, y, z, blockState) -> blockStates.add(blockState),
            DUMMY_REPORTER);

    assertTrue(blockStates.isEmpty());
  }

  @Test
  void loadSubparcelRLE3DInvalid() {
    byte[] data = "000~000\ninvalid\n".getBytes();

    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesRLE3D(
            paletteCtx, data, (x, y, z, blockState) -> blockStates.add(blockState), DUMMY_REPORTER);
    // Should not crash
  }

  @Test
  void loadSubparcelFLATInvalid() {
    byte[] data = "0\ninvalid\n2\n".getBytes();

    List<BlockState> blockStates = new ArrayList<>();
    new ParcellaD32Loader()
        .loadSubparcelBlockStatesFLAT(
            paletteCtx,
            TEST_SUBPARCEL,
            data,
            (x, y, z, blockState) -> blockStates.add(blockState),
            DUMMY_REPORTER);
    // Should not crash
  }
}
