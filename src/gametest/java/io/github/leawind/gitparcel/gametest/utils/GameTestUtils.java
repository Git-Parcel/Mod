package io.github.leawind.gitparcel.gametest.utils;

import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class GameTestUtils {
  private GameTestUtils() {}

  /** Saves with full metadata; the accessor takes the registry access only on 26.1+. */
  public static CompoundTag saveFullMetadata(ServerLevel level, BlockEntity blockEntity) {
    /*? if >=26.1 {*/
    return blockEntity.saveWithFullMetadata(level.registryAccess());
    /*?} else {*/
    /*return blockEntity.saveWithFullMetadata();
     *//*?}*/
  }

  /** CompoundTag key iteration: keySet on 26.x, getAllKeys before that. */
  private static java.util.Set<String> keySetOf(CompoundTag tag) {
    /*? if >=26.1 {*/
    return tag.keySet();
    /*?} else {*/
    /*return tag.getAllKeys();
     *//*?}*/
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

    // Java 17 has no pattern switches: the same shape as an if-else chain with instanceof.
    if (tagA instanceof CompoundTag expCompound) {
      CompoundTag actCompound = (CompoundTag) tagB;
      if (actCompound.size() != expCompound.size()) {
        return false;
      }
      for (String key : keySetOf(expCompound)) {
        Tag actTag = actCompound.get(key);
        if (actTag == null || !compareNbtStructure(expCompound.get(key), actTag, compareListTag)) {
          return false;
        }
      }
      return true;
    } else if (tagA instanceof ListTag expList) {
      if (!compareListTag) {
        return true;
      }
      ListTag actList = (ListTag) tagB;
      if (actList.size() != expList.size()) {
        return false;
      }
      for (int i = 0; i < expList.size(); i++) {
        if (!compareNbtStructure(expList.get(i), actList.get(i), compareListTag)) {
          return false;
        }
      }
      return true;
    }
    return true;
  }
}
