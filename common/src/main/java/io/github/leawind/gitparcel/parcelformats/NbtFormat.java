package io.github.leawind.gitparcel.parcelformats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.inventory.just.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;

public enum NbtFormat {
  Binary(".nbt"),
  Text(".snbt");

  public final String suffix;

  /**
   * Writes the given NBT tag to the specified path in the format of this NBT format.
   *
   * @param path the path to write the NBT tag to. Must exist.
   * @param tag the NBT tag to write.
   * @throws IOException if an I/O error occurs while writing the NBT tag to the path.
   */
  public void write(Path path, CompoundTag tag) throws IOException {
    write(path, tag, false);
  }

  /**
   * Writes the given NBT tag to the specified path in the format of this NBT format.
   *
   * @param path the path to write the NBT tag to. Must exist.
   * @param tag the NBT tag to write.
   * @param format whether to format the NBT tag in a readable way.
   * @throws IOException if an I/O error occurs while writing the NBT tag to the path.
   */
  public void write(Path path, CompoundTag tag, boolean format) throws IOException {
    switch (this) {
      case Binary:
        NbtIo.write(tag, path);
        break;
      case Text:
        if (format) {
          // TODO format snbt
        }
        Files.writeString(path, tag.toString());
        break;
    }
  }

  public Result<CompoundTag, String> read(Path path) {
    return switch (this) {
      case Binary -> readBinary(path);
      case Text -> readText(path);
    };
  }

  NbtFormat(String suffix) {
    this.suffix = suffix;
  }

  public static Result<CompoundTag, String> readBinary(Path path) {
    try {
      return Result.ok(NbtIo.read(path));
    } catch (IOException e) {
      return Result.err(e.getMessage());
    }
  }

  public static Result<CompoundTag, String> readText(Path path) {
    try {
      return Result.ok(TagParser.parseCompoundFully(Files.readString(path)));
    } catch (IOException | CommandSyntaxException e) {
      return Result.err(e.getMessage());
    }
  }
}
