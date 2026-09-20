package io.github.leawind.gitparcel.common.impl.content;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.inventory.just.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

/** Reads and writes record NBT with the limits required for untrusted snapshot data. */
public enum NbtFormat {
  BINARY(".nbt"),
  TEXT(".snbt");

  /** Mirrors {@code ParcelContentFileSupport.MAX_RECORD_FILE_BYTES}. */
  private static final long MAX_RECORD_BYTES = 64L * 1024 * 1024;

  /** Matches the engine's own maximum NBT stack depth ({@code NbtAccounter}). */
  private static final int MAX_SNBT_DEPTH = 512;

  private final String suffix;

  public String getSuffix() {
    return suffix;
  }

  public void write(Path path, CompoundTag tag) throws IOException {
    write(path, tag, true);
  }

  public void write(Path path, CompoundTag tag, boolean format) throws IOException {
    switch (this) {
      case BINARY:
        /*? if >=26.1 {*/
        NbtIo.write(tag, path);
        /*?} else {*/
        /*NbtIo.write(tag, new java.io.DataOutputStream(Files.newOutputStream(path)));
        *//*?}*/
        break;
      case TEXT:
        Files.writeString(path, format ? formatSnbt(tag) : tag.toString());
        break;
    }
  }

  /** Format a NBT tag as pretty-printed SNBT with tab indentation. */
  public static String formatSnbt(Tag tag) {
    var visitor = new SnbtPrinterTagVisitor("\t", 0, new ArrayList<>());
    return visitor.visit(tag);
  }

  public Result<CompoundTag, String> read(Path path) {
    return switch (this) {
      case BINARY -> readBinary(path);
      case TEXT -> readText(path);
    };
  }

  NbtFormat(String suffix) {
    this.suffix = suffix;
  }

  /**
   * Binary NBT is read under a byte-and-depth quota (DESIGN.md path and content safety); vanilla
   * otherwise defaults to {@code NbtAccounter.unlimitedHeap()}.
   */
  public static Result<CompoundTag, String> readBinary(Path path) {
    /*? if >=26.1 {*/
    try (var input = Files.newInputStream(path);
        var data = new java.io.DataInputStream(input)) {
      return Result.ok(NbtIo.read(data, NbtAccounter.create(MAX_RECORD_BYTES)));
    } catch (IOException | net.minecraft.nbt.NbtException e) {
      return Result.err(e.getMessage());
    }
    /*?} else {*/
    /*try (var input = Files.newInputStream(path);
        var data = new java.io.DataInputStream(input)) {
      return Result.ok(NbtIo.read(data, new NbtAccounter(MAX_RECORD_BYTES)));
    } catch (IOException | RuntimeException e) {
      // Pre-26.1 vanilla signals accounter quota and depth rejections as plain RuntimeExceptions.
      return Result.err(e.getMessage());
    }
    *//*?}*/
  }

  /**
   * SNBT has no accounter hook in the engine parser, so the structural depth is bounded before
   * parsing: a deeply nested document would otherwise overflow the parser's recursion stack.
   */
  public static Result<CompoundTag, String> readText(Path path) {
    String text;
    try {
      text = Files.readString(path);
    } catch (IOException e) {
      return Result.err(e.getMessage());
    }
    int depth = structuralDepth(text);
    if (depth > MAX_SNBT_DEPTH) {
      return Result.err(
          "SNBT nesting depth %d exceeds the limit of %d".formatted(depth, MAX_SNBT_DEPTH));
    }
    try {
      /*? if >=26.1 {*/
      return Result.ok(TagParser.parseCompoundFully(text));
      /*?} else {*/
      /*return Result.ok(TagParser.parseTag(text));
      *//*?}*/
    } catch (CommandSyntaxException e) {
      return Result.err(e.getMessage());
    }
  }

  /**
   * Deepest bracket/brace nesting of an SNBT document, ignoring quoted strings and their escapes.
   * Unbalanced documents fail later in the parser.
   */
  private static int structuralDepth(String text) {
    int depth = 0;
    int max = 0;
    boolean quoted = false;
    char quote = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quoted) {
        if (c == '\\') {
          i++;
        } else if (c == quote) {
          quoted = false;
        }
        continue;
      }
      switch (c) {
        case '"', '\'' -> {
          quoted = true;
          quote = c;
        }
        case '[', '{' -> max = Math.max(max, ++depth);
        case ']', '}' -> depth--;
        default -> {}
      }
    }
    return max;
  }
}
