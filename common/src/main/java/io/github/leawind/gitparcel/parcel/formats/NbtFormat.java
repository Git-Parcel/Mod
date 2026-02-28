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

  public void write(Path path, CompoundTag tag) throws IOException {
    write(path, tag, false);
  }

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

  public static CompoundTag readBinary(Path path) throws IOException {
    return NbtIo.read(path);
  }

  public static CompoundTag readReadable(Path path) throws IOException, CommandSyntaxException {
    return TagParser.parseCompoundFully(Files.readString(path));
  }
}
