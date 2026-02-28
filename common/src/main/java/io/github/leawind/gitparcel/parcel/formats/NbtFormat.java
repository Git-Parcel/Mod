package io.github.leawind.gitparcel.parcel.formats;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

  NbtFormat(String suffix) {
    this.suffix = suffix;
  }

  /**
   * Reads an NBT tag from the specified path in binary format.
   *
   * @param path the path to read the NBT tag from.
   * @return the NBT tag read from the path.
   * @throws IOException if an I/O error occurs while reading the NBT tag from the path.
   */
  public static CompoundTag readBinary(Path path) throws IOException {
    return NbtIo.read(path);
  }

  /**
   * Reads an NBT tag from the specified path in text format.
   *
   * @param path the path to read the NBT tag from. Must exist.
   * @return the NBT tag read from the path.
   * @throws IOException if an I/O error occurs while reading the NBT tag from the path.
   * @throws CommandSyntaxException if the NBT tag in the path is not valid.
   */
  public static CompoundTag readText(Path path) throws IOException, CommandSyntaxException {
    return TagParser.parseCompoundFully(Files.readString(path));
  }
}
