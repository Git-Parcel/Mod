package io.github.leawind.gitparcel.parcel.formats.parcella;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.utils.hex.HexUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlockPaletteTest {

  @Test
  void testCollectWithoutNbt() {
    BlockPalette palette = new BlockPalette();

    // Collect same data multiple times should return same id
    int id1 = palette.collect("minecraft:stone", null);
    assertEquals(0, id1); // First item gets id 0
    assertEquals(1, palette.byId.size());
    assertEquals(1, palette.byData.size());

    // Collect same data again should return same id
    int id2 = palette.collect("minecraft:stone", null);
    assertEquals(0, id2); // Same id as before
    assertEquals(1, palette.byId.size()); // Size unchanged
    assertEquals(1, palette.byData.size()); // Size unchanged

    // Collect different data should return new id
    int id3 = palette.collect("minecraft:dirt", null);
    assertEquals(1, id3); // Second item gets id 1
    assertEquals(2, palette.byId.size());
    assertEquals(2, palette.byData.size());

    // Verify data was stored correctly
    BlockPalette.Data data1 = palette.byId.get(0);
    assertEquals("minecraft:stone", data1.blockStateString());
    assertNull(data1.nbt());

    BlockPalette.Data data2 = palette.byId.get(1);
    assertEquals("minecraft:dirt", data2.blockStateString());
    assertNull(data2.nbt());

    // Verify reverse mapping
    assertEquals(Integer.valueOf(0), palette.byData.get(data1));
    assertEquals(Integer.valueOf(1), palette.byData.get(data2));
  }

  @Test
  void testCollectWithNbt() {
    BlockPalette palette = new BlockPalette();

    CompoundTag nbt = new CompoundTag();
    nbt.putString("test_key", "test_value");
    nbt.putInt("test_int", 42);

    int id = palette.collect("minecraft:chest", nbt);
    assertEquals(0, id);

    // Check that the id is in blockEntities set
    assertTrue(palette.blockEntities.contains(0));

    // Verify data was stored correctly
    BlockPalette.Data data = palette.byId.get(0);
    assertEquals("minecraft:chest", data.blockStateString());
    assertNotNull(data.nbt());
    assertEquals("test_value", data.nbt().getString("test_key").get());
    assertEquals(42, data.nbt().getInt("test_int").get());

    // Collect same data with same NBT should return same id
    int sameId = palette.collect("minecraft:chest", nbt);
    assertEquals(0, sameId);

    // Collect same block type but different NBT should return different id
    CompoundTag differentNbt = new CompoundTag();
    differentNbt.putString("different_key", "different_value");
    int differentId = palette.collect("minecraft:chest", differentNbt);
    assertEquals(1, differentId);
    assertTrue(palette.blockEntities.contains(1));
  }

  @Test
  void testClear() {
    BlockPalette palette = new BlockPalette();

    // Add some data
    palette.collect("minecraft:stone", null);
    palette.collect("minecraft:dirt", null);
    CompoundTag nbt = new CompoundTag();
    nbt.putString("key", "value");
    palette.collect("minecraft:chest", nbt);

    // Verify data was added
    assertEquals(3, palette.byId.size());
    assertEquals(3, palette.byData.size());
    assertEquals(1, palette.blockEntities.size()); // Only 1 item has NBT (the chest)

    // Clear the palette
    palette.clear();

    // Verify everything was cleared
    assertEquals(0, palette.byId.size());
    assertEquals(0, palette.byData.size());
    assertEquals(0, palette.blockEntities.size());
  }

  @Test
  void testSaveAndLoadBinary(@TempDir Path tempDir)
      throws IOException,
          BlockPalette.InvalidPaletteException,
          NumberFormatException,
          CommandSyntaxException {
    // Create test palette
    BlockPalette originalPalette = new BlockPalette();

    // Add some data including items with and without NBT
    originalPalette.collect("minecraft:stone", null);
    originalPalette.collect("minecraft:dirt", null);

    CompoundTag chestNbt = new CompoundTag();
    chestNbt.putString("id", "chest");
    chestNbt.putInt("Items", 5);
    originalPalette.collect("minecraft:chest", chestNbt);

    CompoundTag furnaceNbt = new CompoundTag();
    furnaceNbt.putString("id", "furnace");
    furnaceNbt.putInt("BurnTime", 100);
    originalPalette.collect("minecraft:furnace", furnaceNbt);

    // Create directories for saving
    Path paletteFile = tempDir.resolve("palette.txt");
    Path nbtDir = tempDir.resolve("nbt");

    // Save the palette (binary format)
    originalPalette.save(paletteFile, nbtDir, false);

    // Verify files were created
    assertTrue(Files.exists(paletteFile));
    assertTrue(Files.exists(nbtDir));

    // Check palette file contents
    List<String> lines = Files.readAllLines(paletteFile);
    assertEquals(4, lines.size());

    // Parse the palette file to ensure correct format
    assertTrue(
        lines.stream()
            .anyMatch(line -> line.startsWith(HexUtils.toHexUpperCase(0) + "=minecraft:stone")));
    assertTrue(
        lines.stream()
            .anyMatch(line -> line.startsWith(HexUtils.toHexUpperCase(1) + "=minecraft:dirt")));
    assertTrue(
        lines.stream()
            .anyMatch(line -> line.startsWith(HexUtils.toHexUpperCase(2) + "=minecraft:chest")));
    assertTrue(
        lines.stream()
            .anyMatch(line -> line.startsWith(HexUtils.toHexUpperCase(3) + "=minecraft:furnace")));

    // Verify NBT files were created
    assertTrue(Files.exists(nbtDir.resolve("2.nbt")));
    assertTrue(Files.exists(nbtDir.resolve("3.nbt")));
    assertFalse(Files.exists(nbtDir.resolve("0.nbt"))); // No NBT for stone
    assertFalse(Files.exists(nbtDir.resolve("1.nbt"))); // No NBT for dirt

    // Load the palette back
    BlockPalette loadedPalette = null;
    try {
      loadedPalette = BlockPalette.load(paletteFile, nbtDir, false);
    } catch (BlockPalette.InvalidPaletteException e) {
      fail("Failed to load palette: " + e.getMessage());
    }

    // Verify loaded palette matches original
    assertEquals(4, loadedPalette.byId.size());
    assertEquals(4, loadedPalette.byData.size());
    assertEquals(2, loadedPalette.blockEntities.size()); // 2 items had NBT

    // Check individual entries
    BlockPalette.Data stone = loadedPalette.byId.get(0);
    assertNotNull(stone);
    assertEquals("minecraft:stone", stone.blockStateString());
    assertNull(stone.nbt());

    BlockPalette.Data dirt = loadedPalette.byId.get(1);
    assertNotNull(dirt);
    assertEquals("minecraft:dirt", dirt.blockStateString());
    assertNull(dirt.nbt());

    BlockPalette.Data chest = loadedPalette.byId.get(2);
    assertNotNull(chest);
    assertEquals("minecraft:chest", chest.blockStateString());
    assertNotNull(chest.nbt());
    assertEquals("chest", chest.nbt().getString("id").get());
    assertEquals(5, chest.nbt().getInt("Items").get());

    BlockPalette.Data furnace = loadedPalette.byId.get(3);
    assertNotNull(furnace);
    assertEquals("minecraft:furnace", furnace.blockStateString());
    assertNotNull(furnace.nbt());
    assertEquals("furnace", furnace.nbt().getString("id").get());
    assertEquals(100, furnace.nbt().getInt("BurnTime").get());

    // Verify blockEntities set
    assertTrue(loadedPalette.blockEntities.contains(2));
    assertTrue(loadedPalette.blockEntities.contains(3));
    assertFalse(loadedPalette.blockEntities.contains(0));
    assertFalse(loadedPalette.blockEntities.contains(1));
  }

  @Test
  void testSaveAndLoadSNBT(@TempDir Path tempDir)
      throws IOException,
          BlockPalette.InvalidPaletteException,
          NumberFormatException,
          CommandSyntaxException {
    // Create test palette
    BlockPalette originalPalette = new BlockPalette();

    // Add some data including items with NBT
    originalPalette.collect("minecraft:chest", createTestNbt("chest", 5));
    originalPalette.collect("minecraft:furnace", createTestNbt("furnace", 100));

    // Create directories for saving
    Path paletteFile = tempDir.resolve("palette.txt");
    Path nbtDir = tempDir.resolve("nbt");

    // Save the palette (SNBT format)
    originalPalette.save(paletteFile, nbtDir, true);

    // Verify files were created
    assertTrue(Files.exists(paletteFile));
    assertTrue(Files.exists(nbtDir));
    assertTrue(Files.exists(nbtDir.resolve("0.snbt")));
    assertTrue(Files.exists(nbtDir.resolve("1.snbt")));

    // Load the palette back
    BlockPalette loadedPalette = null;
    try {
      loadedPalette = BlockPalette.load(paletteFile, nbtDir, true);
    } catch (BlockPalette.InvalidPaletteException e) {
      fail("Failed to load palette: " + e.getMessage());
    }

    // Verify loaded palette matches original
    assertEquals(2, loadedPalette.byId.size());
    assertEquals(2, loadedPalette.byData.size());
    assertEquals(2, loadedPalette.blockEntities.size());

    // Check individual entries
    BlockPalette.Data chest = loadedPalette.byId.get(0);
    assertNotNull(chest);
    assertEquals("minecraft:chest", chest.blockStateString());
    assertNotNull(chest.nbt());
    assertEquals("chest", chest.nbt().getString("id").get());
    assertEquals(5, chest.nbt().getInt("Items").get());

    BlockPalette.Data furnace = loadedPalette.byId.get(1);
    assertNotNull(furnace);
    assertEquals("minecraft:furnace", furnace.blockStateString());
    assertNotNull(furnace.nbt());
    assertEquals("furnace", furnace.nbt().getString("id").get());
    assertEquals(100, furnace.nbt().getInt("Items").get());
  }

  @Test
  void testLoadOrNewOnError(@TempDir Path tempDir) throws IOException {
    // Test loading from non-existent files should return new palette
    Path nonExistentPalette = tempDir.resolve("nonexistent.txt");
    Path nonExistentNbtDir = tempDir.resolve("nonexistent_nbt");

    BlockPalette palette = BlockPalette.loadOrNew(nonExistentPalette, nonExistentNbtDir, false);
    assertNotNull(palette);
    assertEquals(0, palette.byId.size());
    assertEquals(0, palette.byData.size());
    assertEquals(0, palette.blockEntities.size());

    // Test loading from invalid palette file
    Path invalidPalette = tempDir.resolve("invalid.txt");
    Files.writeString(invalidPalette, "invalid content without equals sign");

    BlockPalette paletteFromInvalid =
        BlockPalette.loadOrNew(invalidPalette, nonExistentNbtDir, false);
    assertNotNull(paletteFromInvalid);
    assertEquals(0, paletteFromInvalid.byId.size());
    assertEquals(0, paletteFromInvalid.byData.size());
    assertEquals(0, paletteFromInvalid.blockEntities.size());
  }

  @Test
  void testLoadInvalidPaletteEntry() throws IOException {
    Path tempDir = Files.createTempDirectory("test");
    try {
      Path paletteFile = tempDir.resolve("palette.txt");
      Path nbtDir = tempDir.resolve("nbt");
      Files.createDirectories(nbtDir);

      // Write an invalid palette entry (missing =)
      Files.writeString(paletteFile, "invalid_entry_without_equals_sign");

      assertThrows(
          BlockPalette.InvalidPaletteException.class,
          () -> {
            BlockPalette.load(paletteFile, nbtDir, false);
          });
    } finally {
      // Clean up temporary directory
      try {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                  } catch (IOException e) {
                    // Ignore cleanup errors
                  }
                });
      } catch (IOException e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  void testLoadDuplicateIds(@TempDir Path tempDir)
      throws IOException,
          BlockPalette.InvalidPaletteException,
          NumberFormatException,
          CommandSyntaxException {
    Path paletteFile = tempDir.resolve("palette.txt");
    Path nbtDir = tempDir.resolve("nbt");
    Files.createDirectories(nbtDir);

    // Write palette with duplicate IDs
    Files.write(
        paletteFile,
        List.of(
            "0=minecraft:stone", "0=minecraft:dirt" // Duplicate ID
            ));

    BlockPalette palette = null;
    try {
      palette = BlockPalette.load(paletteFile, nbtDir, false);
    } catch (BlockPalette.InvalidPaletteException e) {
      fail("Failed to load palette: " + e.getMessage());
    }
    // Should still load, but will have a warning logged (the second entry overwrites the first)
    assertEquals(1, palette.byId.size());
    assertEquals(1, palette.byData.size());
  }

  @Test
  void testDataRecordEqualityAndHashCode() {
    CompoundTag nbt1 = new CompoundTag();
    nbt1.putString("key", "value1");
    CompoundTag nbt2 = new CompoundTag();
    nbt2.putString("key", "value1"); // Same content
    CompoundTag nbt3 = new CompoundTag();
    nbt3.putString("key", "value2"); // Different content

    BlockPalette.Data data1 = new BlockPalette.Data("minecraft:test", nbt1);
    BlockPalette.Data data2 = new BlockPalette.Data("minecraft:test", nbt2);
    BlockPalette.Data data3 = new BlockPalette.Data("minecraft:test", nbt3);
    BlockPalette.Data data4 = new BlockPalette.Data("minecraft:other", nbt1);

    // Same block state string and equivalent NBT should be equal
    assertEquals(data1, data2);
    assertEquals(data1.hashCode(), data2.hashCode());

    // Different NBT should not be equal
    assertNotEquals(data1, data3);

    // Different block state string should not be equal
    assertNotEquals(data1, data4);
  }

  private CompoundTag createTestNbt(String id, int value) {
    CompoundTag nbt = new CompoundTag();
    nbt.putString("id", id);
    nbt.putInt("Items", value);
    return nbt;
  }
}
