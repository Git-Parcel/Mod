package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Matcher for dot-separated NBT paths such as {@code flower_pos} or {@code Items[].tag.target}.
 *
 * <p>Each segment addresses a compound key, {@code []} to iterate every element of a list, or
 * {@code [n]} for a single list index. Paths that cannot be resolved are silently skipped, and so
 * are segments whose container has an unexpected tag type.
 */
public final class NbtPaths {
  private static final int ALL_ELEMENTS = -1;

  /** A writable location inside an NBT tree. */
  public sealed interface Slot {
    Tag get();

    void set(Tag value);

    record CompoundSlot(CompoundTag container, String key) implements Slot {
      @Override
      public Tag get() {
        return container.get(key);
      }

      @Override
      public void set(Tag value) {
        container.put(key, value);
      }
    }

    record ListSlot(ListTag container, int index) implements Slot {
      @Override
      public Tag get() {
        return container.get(index);
      }

      @Override
      public void set(Tag value) {
        container.set(index, value);
      }
    }
  }

  private NbtPaths() {}

  public static List<Object> parse(String path) {
    var segments = new ArrayList<Object>();
    for (String segment : path.split("\\.", -1)) {
      if (segment.isEmpty()) {
        throw new IllegalArgumentException("Empty path segment in: " + path);
      }
      if (segment.endsWith("]")) {
        int opening = segment.indexOf('[');
        if (opening <= 0) {
          throw new IllegalArgumentException("Invalid list segment in: " + path);
        }
        String key = segment.substring(0, opening);
        String index = segment.substring(opening + 1, segment.length() - 1);
        segments.add(key);
        if (index.isEmpty()) {
          segments.add(ALL_ELEMENTS);
        } else {
          try {
            segments.add(Integer.parseInt(index));
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid list index in: " + path, e);
          }
        }
      } else {
        segments.add(segment);
      }
    }
    return List.copyOf(segments);
  }

  /** Visits every location the parsed path resolves to inside {@code root}. */
  public static void forEach(CompoundTag root, List<Object> segments, Consumer<Slot> visitor) {
    if (segments.isEmpty()) {
      return;
    }
    walk(root, segments, 0, visitor);
  }

  private static void walk(Tag current, List<Object> segments, int depth, Consumer<Slot> visitor) {
    if (depth == segments.size()) {
      return;
    }
    Object segment = segments.get(depth);
    if (segment instanceof String key) {
      if (!(current instanceof CompoundTag compound) || !compound.contains(key)) {
        return;
      }
      if (depth == segments.size() - 1) {
        visitor.accept(new Slot.CompoundSlot(compound, key));
      } else {
        walk(compound.get(key), segments, depth + 1, visitor);
      }
    } else if (segment instanceof Integer index) {
      if (!(current instanceof ListTag list)) {
        return;
      }
      if (index == ALL_ELEMENTS) {
        for (int i = 0; i < list.size(); i++) {
          visitOrDescend(list, i, segments, depth, visitor);
        }
      } else if (index >= 0 && index < list.size()) {
        visitOrDescend(list, index, segments, depth, visitor);
      }
    }
  }

  private static void visitOrDescend(
      ListTag list, int index, List<Object> segments, int depth, Consumer<Slot> visitor) {
    @Nullable Tag element = list.get(index);
    if (element == null) {
      return;
    }
    if (depth == segments.size() - 1) {
      visitor.accept(new Slot.ListSlot(list, index));
    } else {
      walk(element, segments, depth + 1, visitor);
    }
  }
}
