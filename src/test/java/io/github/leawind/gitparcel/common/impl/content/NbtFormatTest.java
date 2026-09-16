package io.github.leawind.gitparcel.common.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NbtFormatTest extends AbstractMinecraftTest {
  @TempDir Path tempDir;

  @Test
  void roundTripsNormalDocumentsInBothFormats() throws Exception {
    var tag = new CompoundTag();
    tag.putString("name", "parcel");
    tag.putInt("count", 3);
    var list = new ListTag();
    list.add(IntTag.valueOf(1));
    list.add(IntTag.valueOf(2));
    tag.put("values", list);

    for (NbtFormat format : NbtFormat.values()) {
      Path file = tempDir.resolve("record" + format.getSuffix());
      format.write(file, tag);
      var result = format.read(file);
      assertEquals(tag, result.unwrap());
    }
  }

  @Test
  void rejectsDeeplyNestedSnbtInsteadOfOverflowingTheParser() throws Exception {
    int depth = 2000;
    StringBuilder json = new StringBuilder("{root:");
    for (int i = 0; i < depth; i++) {
      json.append('[');
    }
    json.append("1");
    for (int i = 0; i < depth; i++) {
      json.append(']');
    }
    json.append('}');
    Path file = tempDir.resolve("deep.snbt");
    Files.writeString(file, json.toString());

    var result = NbtFormat.TEXT.read(file);
    assertTrue(result.isErr(), "deep SNBT must be rejected before parsing");
    assertTrue(result.unwrapErr().contains("depth"));
  }

  @Test
  void rejectsDeeplyNestedBinaryNbtThroughTheAccounter() throws Exception {
    CompoundTag nested = new CompoundTag();
    nested.put("value", StringTag.valueOf("bottom"));
    CompoundTag current = nested;
    for (int i = 0; i < 2000; i++) {
      CompoundTag wrapper = new CompoundTag();
      wrapper.put("next", current);
      current = wrapper;
    }
    Path file = tempDir.resolve("deep.nbt");
    NbtFormat.BINARY.write(file, current);

    var result = NbtFormat.BINARY.read(file);
    assertTrue(result.isErr(), "deep binary NBT must be rejected by the accounter");
  }
}
