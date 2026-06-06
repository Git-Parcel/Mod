package io.github.leawind.gitparcel.testutils;

import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.core.api.parcel.config.ConfigItem;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestUtils {
  public static IntIterable iter(int times) {
    return new IntIterable() {
      @Override
      public @NonNull IntIterator iterator() {
        return new IntIterator() {
          int i = 0;

          @Override
          public int nextInt() {
            return i++;
          }

          @Override
          public boolean hasNext() {
            return i < times;
          }
        };
      }
    };
  }

  public interface ParcelFormatCombinationConsumer<F extends ParcelFormat> {
    void accept(F format, Rotation rotation, Mirror mirror) throws Exception;
  }

  public static <F extends ParcelFormat> void forEachFormatCombination(
      Collection<F> formats, ParcelFormatCombinationConsumer<F> consumer) throws Exception {
    for (var format : formats) {
      Rotation[] rotations =
          format.features().contains(ParcelFormat.Feature.ROTATE)
              ? Rotation.values()
              : new Rotation[] {Rotation.NONE};

      Mirror[] mirrors =
          format.features().contains(ParcelFormat.Feature.MIRROR)
              ? Mirror.values()
              : new Mirror[] {Mirror.NONE};

      for (Rotation rotation : rotations) {
        for (Mirror mirror : mirrors) {
          consumer.accept(format, rotation, mirror);
        }
      }
    }
  }

  public static <C extends ParcelFormatConfig<C>> List<Map<String, ?>> generateConfigCombinations(
      ParcelFormat.Impl<C> format) {
    Map<String, List<?>> options = new HashMap<>();
    var defaultConfig = format.getDefaultConfig();

    if (defaultConfig == null) {
      return List.of(Map.of());
    }

    for (var item : defaultConfig.listConfigItems()) {
      List<?> testValues = getTestValues(item);
      if (testValues.size() > 1) {
        options.putIfAbsent(item.name(), testValues);
      }
    }

    if (options.isEmpty()) {
      return List.of(Map.of());
    }

    return cartesianProduct(options);
  }

  public static List<Map<String, ?>> cartesianProduct(Map<String, List<?>> map) {
    if (map.isEmpty()) {
      return List.of();
    }

    List<Map<String, ?>> result = new ArrayList<>();

    List<Map.Entry<String, List<?>>> entries = new ArrayList<>(map.entrySet());

    List<?> firstValues = entries.getFirst().getValue();
    for (var value : firstValues) {
      Map<String, Object> m = new HashMap<>();
      m.put(entries.getFirst().getKey(), value);
      result.add(m);
    }

    for (int i = 1; i < entries.size(); i++) {
      var entry = entries.get(i);
      String key = entry.getKey();
      List<?> values = entry.getValue();

      List<Map<String, ?>> newResult = new ArrayList<>();
      for (var current : result) {
        for (var value : values) {
          Map<String, Object> temp = new HashMap<>(current);
          temp.put(key, value);
          newResult.add(temp);
        }
      }
      result = newResult;
    }

    return result;
  }

  /**
   * Returns candidate test values for a config item.
   *
   * <ul>
   *   <li>For enums: all enum constants
   *   <li>For booleans: {@code true} and {@code false}
   *   <li>For other types: only the default value (prevents combinatorial explosion)
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> getTestValues(ConfigItem<T> item) {
    var defaultValue = item.defaultValue();
    if (defaultValue instanceof Enum<?> defaultValueEnum) {
      return (List<T>) List.of(defaultValueEnum.getDeclaringClass().getEnumConstants());
    } else if (defaultValue instanceof Boolean) {
      return (List<T>) List.of(true, false);
    } else {
      return List.of(defaultValue);
    }
  }

  public static boolean compareNbtStructure(
      @Nullable Tag tagA, @Nullable Tag tagB, boolean compareListTag) {
    if (tagA == tagB) {
      return true;
    }
    if (tagA == null || tagB == null) {
      return false;
    }
    if (!tagA.getClass().equals(tagB.getClass())) {
      return false;
    }

    return switch (tagA) {
      case CompoundTag expCompound -> {
        CompoundTag actCompound = (CompoundTag) tagB;
        if (actCompound.size() != expCompound.size()) {
          yield false;
        }
        for (var entry : expCompound.entrySet()) {
          Tag actTag = actCompound.get(entry.getKey());
          if (actTag == null || !compareNbtStructure(entry.getValue(), actTag, compareListTag)) {
            yield false;
          }
        }
        yield true;
      }
      case ListTag expList -> {
        if (!compareListTag) {
          yield true;
        }
        ListTag actList = (ListTag) tagB;
        if (actList.size() != expList.size()) {
          yield false;
        }
        for (int i = 0; i < expList.size(); i++) {
          if (!compareNbtStructure(expList.get(i), actList.get(i), compareListTag)) {
            yield false;
          }
        }
        yield true;
      }
      default -> true;
    };
  }
}
