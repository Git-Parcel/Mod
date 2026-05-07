package io.github.leawind.gitparcel.parcelformats.parcella;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.gitparcel.testutils.AbstractGitParcelTest;
import io.github.leawind.inventory.just.Result;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class BlockPaletteTest extends AbstractGitParcelTest {

  @Test
  void parseWithMinecraftNamespace() {
    var result = BlockPalette.parseBlockState("minecraft:stone");
    assertInstanceOf(Result.Ok.class, result);
    assertEquals(
        Blocks.STONE.defaultBlockState(), ((Result.Ok<BlockState, String>) result).value());
  }

  @Test
  void parseWithoutMinecraftNamespace() {
    var result = BlockPalette.parseBlockState("stone");
    assertInstanceOf(Result.Ok.class, result);
    assertEquals(
        Blocks.STONE.defaultBlockState(), ((Result.Ok<BlockState, String>) result).value());
  }

  @Test
  void parseWithAndWithoutNamespaceAreEqual() {
    var resultWith = BlockPalette.parseBlockState("minecraft:stone");
    var resultWithout = BlockPalette.parseBlockState("stone");

    assertInstanceOf(Result.Ok.class, resultWith);
    assertInstanceOf(Result.Ok.class, resultWithout);

    var stateWith = ((Result.Ok<BlockState, String>) resultWith).value();
    var stateWithout = ((Result.Ok<BlockState, String>) resultWithout).value();
    assertEquals(stateWith, stateWithout);
  }

  @Test
  void parseWithPropertiesWithNamespace() {
    var result = BlockPalette.parseBlockState("minecraft:campfire[lit=true,facing=north]");
    assertInstanceOf(Result.Ok.class, result);
  }

  @Test
  void parseWithPropertiesWithoutNamespace() {
    var result = BlockPalette.parseBlockState("campfire[lit=true,facing=north]");
    assertInstanceOf(Result.Ok.class, result);
  }

  @Test
  void parseWithPropertiesNamespaceOmissionIsEqual() {
    var resultWith = BlockPalette.parseBlockState("minecraft:oak_fence[waterlogged=true]");
    var resultWithout = BlockPalette.parseBlockState("oak_fence[waterlogged=true]");

    assertInstanceOf(Result.Ok.class, resultWith);
    assertInstanceOf(Result.Ok.class, resultWithout);

    var stateWith = ((Result.Ok<BlockState, String>) resultWith).value();
    var stateWithout = ((Result.Ok<BlockState, String>) resultWithout).value();
    assertEquals(stateWith, stateWithout);
  }

  @Test
  void parseReturnsErrForNonexistentBlockWithNamespace() {
    var result = BlockPalette.parseBlockState("minecraft:nonexistent_block");
    assertInstanceOf(Result.Err.class, result);
  }

  @Test
  void parseReturnsErrForNonexistentBlockWithoutNamespace() {
    var result = BlockPalette.parseBlockState("nonexistent_block");
    assertInstanceOf(Result.Err.class, result);
  }

  @Test
  void parseReturnsErrForInvalidSyntax() {
    var result = BlockPalette.parseBlockState("stone[invalid");
    assertInstanceOf(Result.Err.class, result);
  }

  @Test
  void stringifySimpleBlockState() {
    var state = Blocks.STONE.defaultBlockState();
    var stringified = BlockPalette.stringifyBlockState(state);
    assertEquals("minecraft:stone", stringified);
  }

  @Test
  void roundTripSimpleBlockState() {
    var original = Blocks.STONE.defaultBlockState();
    var stringified = BlockPalette.stringifyBlockState(original);
    var parsed = BlockPalette.parseBlockState(stringified);

    assertInstanceOf(Result.Ok.class, parsed);
    assertEquals(original, ((Result.Ok<BlockState, String>) parsed).value());
  }
}
